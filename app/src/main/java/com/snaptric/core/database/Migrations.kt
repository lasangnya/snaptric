package com.snaptric.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.units.MeterUnit

/**
 * v2 → v3: adds the tariffs table, and rewrites each meter's unit to its canonical symbol
 * ("m3" → "m³", "kwh" → "kWh") so conversions recognise it. A missing unit gets the usual one for
 * the meter type (gas and water m³, electricity kWh); units that aren't recognised are kept as typed.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tariffs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `utilityId` INTEGER NOT NULL, " +
                "`unitRate` REAL NOT NULL, `billingUnit` TEXT NOT NULL, `standingChargePerDay` REAL NOT NULL, " +
                "`currency` TEXT NOT NULL, `effectiveFrom` INTEGER NOT NULL, `calorificValue` REAL, " +
                "`volumeCorrection` REAL, `source` TEXT NOT NULL, " +
                "FOREIGN KEY(`utilityId`) REFERENCES `utility`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tariffs_utilityId` ON `tariffs` (`utilityId`)")

        val updates = mutableListOf<Pair<Long, String>>()
        db.query("SELECT id, type, unit FROM utility").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val unit = cursor.getString(2).orEmpty()
                val canonical = canonicalUnit(cursor.getString(1), unit)
                if (canonical != unit) updates += id to canonical
            }
        }
        updates.forEach { (id, unit) ->
            db.execSQL("UPDATE utility SET unit = ? WHERE id = ?", arrayOf<Any>(unit, id))
        }
    }
}

/** The unit a meter of [type] should have after migration, given its stored [unit]. */
internal fun canonicalUnit(type: String, unit: String): String {
    MeterUnit.parse(unit)?.let { return it.symbol }
    if (unit.isNotBlank()) return unit
    val utilityType = UtilityType.entries.firstOrNull { it.name == type } ?: return unit
    return MeterUnit.defaultFor(utilityType).symbol
}
