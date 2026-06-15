# 📸 Snaptric 
> **AI-Powered Utility Tracking for the Modern Home.**

**Snaptric** is a personal Android application I built to solve a real-world problem: the tedious task of manually tracking household gas, electricity, and water consumption. By combining **CameraX** with **Google ML Kit**, this app allows you to simply point your camera at a meter and capture a reading in seconds.

This project is open-source and open to anyone who wants a private, local-first way to manage their household data.

---

## ✨ Features

### **Current Capabilities**
- 🏠 **Multi-Property Support**: Manage utilities across different locations (e.g., "Home," "Office," "Summer House").
- ⚡ **Dynamic Utility Management**: Add specific meters (Electricity, Gas, Water) with customizable units (kWh, m³, Liters, etc.).
- 📷 **Smart Viewfinder**: A custom-built camera overlay with a precise rectangle cutout to ensure high-accuracy OCR.
- 🤖 **AI Reading Extraction**: Automatically detects digits from your meter using **Google ML Kit Text Recognition**.
- 🛠 **Physical Image Cropping**: To ensure maximum reliability, the app physically crops the image to the viewfinder bounds before analysis, eliminating background noise like serial numbers or logos.
- 📂 **Local-First Persistence**: Powered by **Room Database**, ensuring your data stays on your device.

### **Roadmap**
- 🧠 **On-Device LLM (Gemma)**: Future integration of Google’s **Gemma** model via MediaPipe to provide "Smart Mode" for difficult-to-read analog dials and low-light environments.
- 📊 **Consumption Analytics**: Visual charts to track usage trends over months.
- 🔔 **Monthly Reminders**: Integration with WorkManager to send "Reading Day" notifications.

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose (100%) with Material 3.
- **Architecture**: Clean Architecture with a multi-module intent (Core/Feature separation).
- **Dependency Injection**: Hilt.
- **Database**: Room (Relational schema: Property → Utility → Reading).
- **Camera**: CameraX (with ImageProxy-to-Bitmap processing).
- **AI/ML**: Google ML Kit (Vision).
- **Async**: Kotlin Coroutines & Flow.

---

## 🏗 Architecture

The project follows modern Android development patterns to ensure the code is testable and scalable:

- **State Hoisting**: Screens are separated into `Stateful` (Hilt/ViewModel) and `Stateless` (UI Layout) components, allowing for robust **Compose Previews**.
- **Relational Integrity**: Uses Foreign Keys and `CASCADE` deletes to ensure that deleting a property automatically cleans up all associated meters and readings.
- **Image Processing Pipeline**: Custom logic to handle bitmap rotation and center-strip cropping based on device orientation.

---

## 🚀 Getting Started

1. **Clone the repo**:
   ```bash
   git clone https://github.com/lasangnya/snaptric.git
   ```
2. **Open in Android Studio**: Use Ladybug or newer.
3. **Run on Device**: A physical device is required to test the CameraX and ML Kit features accurately.

---

## 💡 Motivation
I created Snaptric because I wanted a tool that was faster than a spreadsheet but more private than a cloud-based utility app. It started as a way to track my own household meters and evolved into a showcase of how on-device AI can simplify everyday chores.

---

## ⚖️ License
Distributed under the MIT License. See `LICENSE` for more information.

---
*Developed with ❤️ for the Android Community.*
