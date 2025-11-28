* Charts:
    * Extra range axes
    * Axes dragging and scaling with mouse wheel (or Ctrl/Command key)
* Fix error colors in the certain Strings in results  
* Fix unexpected focus change and incorrect server drop down selection
* Pop-up menu in toolbar buttons for navigation history for previous/next server
* Adding additional L&F including dark schemas
* Adding Settings of colors and font style for syntax highlighting and for result grids

`dz6.0` 2025.08.08
-----
* UI for TLS connections
    * Secure status in the status bar
    * Certificate chain dialog
    * Support untrusted TLS certificates
    * Manage CA roots
* Setting to inspect results in the same tab with navigation functionality 
* Fix issue with incorrect table's column width calculation on Windows with scaled display
    * Column width is also adjusted now after altering decimal formatting
* Setting to open servers from the result grid popup menu in the current editor tab
* Adding server back/forward actions which also work with mouse back/forward buttons
* Chart merging
* Charting shortcuts:
    * Ctrl + G (Command + G for MacOS) is to chart
    * Ctrl + Shift + E / Ctrl + Shift + Enter (Command for MacOS) is to execute and chart
* Import/export server list
* Comment line action - Ctrl + / (or Command + / on MacOS) 
* Chart Settings for color schema and stroke styles
* Configuration is stored in json format (studio.json)
    * Details of server connections are extracted into separate servers.json config file
    * Defaults for studio.json can be put into plugins folder

`dz5.0` 2025.02.28
-----
* Fixes for atom formating:
    * datetime older than 1970 year
    * adding 0D for timespan
    * syntax highlighting for negative timespan
* Adding keep alive for all connections
* Line tool is added to chart dialog
* Adding drag&drop in the tree of servers (in the Server List dialog)
* Server connect/disconnect actions from the status bar
    * adding selection of auth.method into the status bar 
* Server Add form prepopulate details of currently selected server
* Export as Excel
    * Fixing timezone bug
    * Adding server and query details into a separate tab
* Any IO errors are added as Error tab (pop-up error message is used for internal bugs)
    * KX errors from servers are now display with ' (not as symbol)
* Parent folder can be specified in the server form (into Add and Edit server forms)
* Persist location of the server list dialog for every Studio window

`dz4.1` 2024.01.29
-----
* Added line inspection (new popup menu in the table result)
* New setting to disable insert of paired character in the editor
* Implement continuous search
    * Fixed the bug with sequential replace action in the editor

`dz4.0` 2024.01.09
-----
* Drill down for complex results (with double click or popup menu)
* Rework kdb connection logic
    * No IPC protocol downgrade in case of first attempt failure
    * Fix logic to detect server disconnect
    * Connection status added to the status bar
    * Capturing connection statistics (new menu action)
    * Limit on incoming message (new settings)
    * Adding more details in the tooltip of the result tab
* File drag and drop (no support for a folder drag and drop) 
* Persist location of editor windows
* Keep config properties sorted on disk
* Split editor tabs

`dz3.0.1` 2023.09.22
-----
* Bugfix to enable "Upload" button when result tabs are switched
* Backup of config and workspace files for 2 weeks

`dz3.0` 2023.08.04
-----
* Select columns in result table by clicking headers with ALT and SHIFT
* Added config to close not saved anonymous tabs on exit
* Added search functionality for result tables
* Show timer during query execution
* Added settings for controlling kdb connection reuse and invalidation 
* Added font selection for the editor and result table
    * Font size is changed with mouse wheel and pressed Ctrl (Command for MacOS)
* Tab emulation with spaces
    * Action to convert tab to spaces
    * Adding settings for tab emulation
* Pinning result tabs
* KDBSTUDIO\_CONFIG\_HOME environment variable overrides location of Studio home with configs and logs
* Improvements in charting: 
    * display mouse cursor coordinates
    * drag for zoom can be done in any direction
    * chart grid can be moved
    * shortcuts for copy, save, etc.

`dz2.0.1` 2021.12.23
-----
* Upgraded log4j to the latest version
* Add support for MacOS Preferences/About/Quit native menu in Java 9+
* Monitoring file changes on disk to reloaded into editor

`dz2.0` 2021.12.01
-----
* Double-click in result grid:
    * beside system double-click, added emulated double-click with configurable speed
    * ALT + mouse click also copies the cell
* (Windows only) Icon grouping in taskbar
    * grouping are different to other application started with the same Java machine
    * grouping for editor and charting frames are different
* Number of fractional digits in output is now configurable 
* Adding mnemonic shortcuts in confirmation dialogs
* Support of different line ending styles
* Adding SaveAll action
* Adding (*) to the title if the tab has modified content
* Adding tooltip into output tab with server where the query was executed

`dz1.11` 2021.09.24
-----
* Output grid:
    * add vertical lines and make header compact
    * Settings: adding right padding and max width for columns
    * trying to keep old order during sorting
    * sorting row header as well
* Added shortcut to select next and previous editor tab
* Reworked charts:
    * added shapes, strokes and bars
    * legends with colors and char types for individual charts
    * zoom with mouse wheel
    * chart frame title is derived from chart title

`dz1.10` 2021.06.30
-----
* Open - changed behaviour to open in a new tab
* Settings: auto save modified files
* Settings: exit without asking to save anything
* Upload result to a server
* Drag and Drop for editor and result tabs

`dz1.9` 2021.04.28
-----
* Manually closure of tabs
    * Middle-click to close tab or right-click for a popup menu  
* Pop up menu in the result table can include actions to open servers
    * The decision is made by values in current row and selected cell 
* Restoring windows and tabs from previous application run
    * The state is also persisted every minute  
* Show an About dialog on start up if release notes are changed
* Options to execute all script when nothing is selected. The default option is to ask
    * The option is added to Settings
* Add notes into About dialog
* Add support for multiple tabs in a StudioPanel:
    * Ctrl + N (Command + N) opens a new tab
    * Ctrl + Shift + N (Command + Shift + N) opens a new window

`dz1.8` 2021.03.30
-----
* Fix memory leak related to keeping previously loaded results
* Fix formatting of Composition type in the result output
* Import servers from QPad (http://www.qinsightpad.com/)

`dz1.7` 2021.01.19
-----
* Add history of servers opened in StudioPanel
* Fix syntax highlighting for communication handle symbol (like `:server:port )
* Add Log4j 2 for logging application and queries to $HOME/.studioforkdb/log folder
* Set new syntax highlighting in Console result pane

`dz1.6` 2020.12.22
-----
* Rework q syntax highlighting
* Update versioning and About Dialog. Added release notes into notes.md 

`dz1.5` 2020.12.04
-----
* Customization of output format with comma thousands separator
* Bugfix for not starting Studio without config file
* Copy and cut action adds syntax highlighting into clipboard

`dz1.4` 2020.10.19
-----
* Double click in result table cells copies content into clipboard
* Remove zero char when copying into clipboard
* Fix formatting of projections

`dz1.3` 2020.06.04
-----
* Syntax highlighting in output result
* Multiple tabs in the result pane
* Selection and Look and Feel

`dz1.2` 2020.05.01
-----
* Hide drop down servers option
* Tree view for Server list
* Add/remove line in charts
* Copy as HTML

`dz1.1` 2020.04.10
-----
* Text field with connection details
* Added Settings menu
* Bugfix for loading custom authentication plugin
* Dictionary and list are displayed as table
* Added server list
* Added formatting for `binr`, `cov`, `cor` (BinaryPrimitive) and `var`, `dev`, `hopen` (UnaryPrimitive)


`3.35` 2020.01.24
-----
The version which was forked from