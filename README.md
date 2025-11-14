## Features

The `HelpScreen` is designed to guide the user through the app's functionalities. It is divided into several collapsible sections, each addressing a specific aspect of the application:

*   **❓ Help & Support Center:** A header that introduces the purpose of the screen.
*   **🚀 Getting Started Guide:** A step-by-step tutorial for new users, covering the creation of locations, building a storage hierarchy, and adding and finding items.
*   **👜 Managing Your Items:** Explanations of key item management features like Auto-Save, OCR (Optical Character Recognition), and AI-powered item recognition. It also details how to add custom dropdown options.
*   **💾 Data, Backup & Sync:** Information on local data storage, and detailed instructions on how to back up, restore, and sync inventory data with a computer application.
*   **⚙️ Advanced Settings & APIs:** Details for advanced users about API key integration for enhanced OCR and AI functionalities, and an explanation of the priority fallback system.
*   **ℹ️ App Information and Contact:** A dedicated card that displays app details and provides contact information for the developers, including links to their GitHub profiles and email addresses.

## UI and Code Structure

The screen is built with a `LazyColumn` to efficiently display a scrollable list of items. The main components are:

### Core Composables

*   `HelpScreen()`: The main composable function that assembles the entire screen.
*   `CollapsibleHelpSection()`: A reusable composable that creates a clickable, expandable card. This is used for each major help topic, allowing users to show or hide content as needed.
*   `HelpContentBlock()`: A simple composable for formatting the question-and-answer style content within each collapsible section.
*   `ContactRow()`: A row designed to display contact information, complete with an icon, title, and a clickable subtitle that opens an external link (GitHub or email).
*   `InfoRow()`: A composable for displaying key-value information, used in the "App Information" section.

### Key Functionality

*   **State Management:** The expanded/collapsed state of each `CollapsibleHelpSection` is managed using `remember { mutableStateOf(...) }`.
*   **Animation:** The expansion and collapse of sections are animated using `AnimatedVisibility`. The rotation of the expand/collapse icon is animated with `animateFloatAsState`.
*   **Intents:** The contact section uses Android `Intent`s to open web pages (`ACTION_VIEW`) and email clients (`ACTION_SENDTO`) when a user clicks on the respective links.

## Contact & Credits

This screen was developed by:

*   **Parminder**
    *   GitHub: [JohnJackson12](https://github.com/JohnJackson12)
    *   Email: [parminder.nz@gmail.com](mailto:parminder.nz@gmail.com)
*   **Samuel**
    *   GitHub: [SamS34](https://github.com/SamS34)
    *   Email: [sam.of.s34@gmail.com](mailto:sam.of.s34@gmail.com)
