# StickyNoteApp

A lightweight, customizable sticky notes application built with Java Swing that runs in your system tray. I really wanted a sticky note app where I could control its transparency and have it lock to the top layer of the screen above applications, so I did it myself!

With Controls Visible:
![image](https://github.com/user-attachments/assets/88015fbe-ec14-4a6c-bf48-92d9c756bf50)

With Controls Hidden:
![image](https://github.com/user-attachments/assets/fada01f8-e88f-499b-8f9e-3455f43d07e8)


## Features

- **System Tray Integration**: Access your notes from the system tray without cluttering your desktop
- **Rich Text Formatting**: Apply bold, italic, custom fonts, and colors to your notes
- **Customizable Appearance**: Change background color, toolbar color, and transparency for each note
- **Note Management**: Create, edit, resize, and organize your sticky notes
- **Always-on-Top Option**: Keep important notes visible above other windows
- **Auto-Save**: All changes are automatically saved to disk
- **Global Settings**: Define default appearance settings for all new notes
- **Resizable Notes**: Adjust note dimensions via edge resizing or precise pixel settings
- **Transparency Control**: Set transparency levels for better desktop integration

## Getting Started

### Prerequisites

- Java Runtime Environment (JRE) 8 or later

### Installation

1. Download the latest release JAR file from the [Releases](https://github.com/YourUsername/StickNoteApp/releases) page
2. Double-click the JAR file to run the application
3. The application will appear in your system tray

### Building from Source

1. Clone this repository:
   ```
   git clone https://github.com/YourUsername/StickNoteApp.git
   ```
2. Open the project in your preferred Java IDE (Eclipse, IntelliJ, etc.)
3. Build the project
4. Run the `Main` class

## Usage

### Creating a New Note
- Right-click the system tray icon and select "New Note"
- A new sticky note will appear on your desktop
![image](https://github.com/user-attachments/assets/15c0e84a-9963-42db-a20b-82fd3385bd59)


### Formatting Text
- Use the toolbar buttons to apply formatting:
  - **B**: Toggle bold text (Double-click to set bold typing mode)
  - **I**: Toggle italic text (Double-click to set italic typing mode)
  - **•**: Insert a bullet point
  - **A**: Change font family
  ![image](https://github.com/user-attachments/assets/f8a03517-f51a-417b-9c30-72da0cfc0036)

  - **Color**: Change text color
  ![image](https://github.com/user-attachments/assets/1700955f-2606-4f1d-8b96-3df53fdb1b13)

  - **Size**: Change font size
  ![image](https://github.com/user-attachments/assets/13be57a5-6029-4448-9c19-4bcd6fe09c8e)


### Note Controls
- **Close**: Hide the note (still accessible from notes list)
- **Settings**: Access note-specific settings
- **Pin**: Toggle always-on-top mode
- **Lock**: Prevent accidental edits

### Note Settings
![image](https://github.com/user-attachments/assets/d68d0050-d161-4414-8335-6139745db17f)

- Change background color
- Change toolbar color
- Adjust transparency
- Set exact width and height
- Delete the note

### Global Settings
![image](https://github.com/user-attachments/assets/3c36e4c9-ecce-4ff8-8a50-3e76095c2935)

- Set default background color for new notes
- Set default toolbar color for new notes
- Set default font family and size
- Change data storage location

### Managing Notes
![image](https://github.com/user-attachments/assets/6517cff7-decd-44ed-a1b5-f86b133d8a84)

- Access all notes via the "Notes List" option in the system tray menu
- Open or delete notes from the list

## Application Structure

The application is organized into several key classes:

- `Main`: Entry point with system tray setup
- `NotesManager`: Handles note creation, storage, and retrieval
- `NoteWindow`: The UI for individual sticky notes
- `NoteData`: Data model for individual notes
- `AppSettings`: Global application settings
- `NoteSettingsWindow`: UI for note-specific settings
- `GlobalSettingsWindow`: UI for application-wide settings

## Data Storage

Notes and settings are stored in two property files:
- `notes_data.properties`: Contains all note content and individual settings
- `global_settings.properties`: Contains application-wide default settings


## Acknowledgments

- Developed by Dominic Minnich
- Icons by Icons8 Pichon

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
