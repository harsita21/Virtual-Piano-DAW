# 🎹 Virtual Piano DAW

A Java-based Virtual Piano and Mini-Digital Audio Workstation (DAW). This application allows users to play a virtual piano using their mouse, record their sessions, and manage their recordings with a simple graphical user interface.

## 📽️ Demonstration

**[Watch Demo](https://drive.google.com/file/d/1C9NBPV-_MbS7zDTleflCXtOdQAYadHei/view?usp=sharing)**

---

## ✨ Features

* **Virtual Piano:** 3-Octave playable piano keyboard (White and Black keys).
* **Audio Synthesis:** Real-time MIDI note generation.
* **Recording:** Record your piano sessions and save them as `.wav` files.
* **Playback:** Play back your recorded sessions directly within the app.
* **File Management:**
  * Rename recordings.
  * Delete unwanted recordings.
* **Tempo Control:** Adjustable BPM (Beats Per Minute).

## 🚀 How to Run

### Prerequisites

* Java Development Kit (JDK) installed (Java 8 or higher).

### Installation & Execution

1. **Download** the `VP_DAW.java` file.
2. Open your terminal or command prompt in the folder containing the file.
3. **Compile** the code:
   ```bash
   javac VP_DAW.java
4.  **Run** the application:
    ```bash
    java VP_DAW
    ```

## 📖 Usage Guide
1.  **Playing:** Click on any key (White or Black) to play a note.
2.  **Recording:**
    * Click the `⏺ Record` button to start. The button will turn red.
    * Play your melody.
    * Click `⏹ Stop` to finish. The recording will automatically save with a timestamp.
3.  **Managing Files:**
    * Select a file from the "Saved Recordings" list on the right.
    * Use the **Play**, **Rename**, or **Delete** buttons to manage the selected file.

## 🛠️ Technical Details
* **Language:** Java
* **GUI Framework:** Java AWT (Abstract Window Toolkit)
* **Audio:** Java MIDI System (`javax.sound.midi`) and Audio System (`javax.sound.sampled`)
