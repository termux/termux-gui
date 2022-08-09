# Protocol

If you don't want to implement a custom library for using the protocol, you can skip to the [protocol methods](#methods)<!-- @IGNORE PREVIOUS: anchor -->


Due to Android limitations, methods that return a value fail when the Activity isn't visible and up to 100 methods that don't return a value will be queued up to run when the Activity is visible again.  
For the binary protocol no methods will be queued, since they all return a value.  
If onCreate gets called again for an Activity (because it was destroyed and re-created by the system (this shouldn't happen often because the Activities ignore all configuration changes), all state in that Activity like registered listeners, and views is lost.  
You should always store state you care about yourself, because Android reserves the right to terminate an Activity or a process at any time, and rebuild the layout when you see that a second onCreate is triggered for one of your activities.  

## Connection


The program has to open 2 AF_UNIX SO_STREAM server sockets in the abstract linux namespace and listen to new connections.  
One of these Sockets is used as the main communication socket with the plugin, the other one is used to send asynchronous event data to the program, eg. click events.  
  
The Program then has to send a broadcast to com.termux.gui/.GUIReceiver with the string extra mainSocket and the string extra eventSocket, the values of which specify the socket name used for that communication type. The names have to be transferred without the leading null byte required to specify the abstract linux namespace.  
Delivering the broadcast can be easily done with the `am` command:  
`am broadcast --user 0 -n com.termux.gui/.GUIReceiver --es mainSocket mainSocketName --es eventSocket eventSocketName`  
  
For additional security, the program may check if the connected peer has the same user id as the program itself, to ensure only the plugin can accept the connection.

## Protocol types

2 Protocol types are supported right now:
- JSON: For better compatibility with high-level languages, messages can be transferred in JSON.
- Binary: For Increased throughput and compatibility with low-level languages, a protobuf-based protocol can be used. This will eventually be the default and the JSOn protocol will be removed.

For the JSON Protocol, each message must be preceded by the length of the message (without this length value) as a 4 byte unsigned integer, the same with the return messages from the plugin.
This integer is send big-endian.

## Protocol negotiation

The first byte send specifies the desired protocol type and version:  
- The 4 most significant bits specify the protocol type: 0 indicates the binary protocol, 1 indicates JSON.
- The 4 least significant bits specify the protocol version. Currently unused, should be set to 0 for compatibility with future versions.

The plugin then responds with a single byte unsigned integer. A response of 0 means the plugin supports the desired protocol type and version. Any other value denotes an error and the plugin will close the connection.

## <a name="methods"></a>Protocol methods

Each message invokes a method on the plugin.  
Methods may return values, the number of return values can vary with the parameters.  
  
Available methods:  

Activity and Task control:  
These methods control Android [Activities](https://developer.android.com/reference/android/app/Activity) and [Tasks](https://developer.android.com/guide/components/activities/tasks-and-back-stack). Also here are methods that don't need an Activity or Task.
- newActivity: Launches a new Activity and returns the activity id. In case of an error, -1 is returned.
  - Parameters:
    - tid: the task in which the Activity should be started. If not specified, a new Task is created and the id is returned after the Activity id.
    - One of:
      - dialog: boolean value. If true, the Activity will be launched as a dialog.
      - canceloutside: boolean value, optional. Sets whether the Activity is destroyed when it is displayed as a dialog and the user clicks outside. Default is true.
      - pip: boolean. Whether or not to start the Activity in [Picture-in-Picture mode](https://developer.android.com/guide/topics/ui/picture-in-picture). Default is false. This should only be used to create Activities in a new Task.
      - lockscreen: displays this Activity on the lockscreen.
      - overlay: uses a system overlay window to display above everything else. Overlays are never in a Task and creating one doesn't return a Task id. Overlays don't get Activity lifecycle events.
    - intercept: A boolean, whether to intercept back button presses and send an event or to use the default action of finishing the Activity. Default value is false.
- finishActivity: Closes an Activity.
  - Parameters:
     - aid: The id of the Activity to close.
- finishTask: Closes a Task including all activities in it.
  - Parameters:
    - tid: The id of the Task to close.
- bringTaskToFront: Shows a Task to the user if it wasn't already.
  - Parameters:
    - tid: The id of the Task to show.
- moveTaskToBack: Makes the Task go into the background.
  - Parameters:
    - aid: The id of an Activity in the Task.
- setTheme: Sets the theme for the activity. It only applies to newly created Views, so this should be set before any Views are added. The color has to be specified as an RGBA8888 integer (in hex literals 0xaabbggrr).
  - Parameters:
    - aid: The Activity id.
    - statusBarColor
    - colorPrimary
    - windowBackground
    - textColor
    - colorAccent
- setTaskDescription: Sets the Icon of the Task, the label and set the primary color to the one specified with setTheme. The background color of the icon will be the current primary color.
  - Parameters:
    - aid: The id of an Activity in the Task you want to set the icon.
    - img: The image data in PNG or JPEG format. May also be the literal string "default". to reset to the default Termux icon.
    - label: The new Task label.
- setPiPParams: Sets the PiP parameters for the Activity. PiP parameters are only available on Android 8+, on earlier versions this is a noop.
  - Parameters:
    - aid: The id of an Activity in the Task you want to set the icon.
    - num: Numerator of the desired aspect ration.
    - den: Denominator of the desired aspect ration.
- setInputMode: Determines what happens when the virtual keyboard is shown.
  - Parameters:
    - aid: The id of the Activity.
    - mode: one of: "resize": resizes the Activity, "pan": pans the Activity.
- setPiPMode: Makes an Activity enter or exit pip mode.
  - Parameters:
    - aid: The id of the Activity.
    - pip: Whether the Activity should be in pip mode or not. Exiting pip mode makes the Activity a background task, that is it's not immediately shown fullscreen to the user.
- setPiPModeAuto: Whether or not an Activity should automatically enter pip mode when the user navigates away.
  - Parameters:
    - aid: The id of the Activity.
    - pip: Whether or not an Activity should automatically enter pip mode when the user navigates away.
- keepScreenOn: Sets whether the screen should be kept on when an Activity shows.
  - Parameters:
    - aid: The id of the Activity.
    - on: Whether the screen should be kept on.
- setOrientation: Set the screen orientation (only when the activity is shown).
  - Parameters:
    - aid: The id of the Activity.
    - orientation: Can be one of the values of the constant column in [this table](https://developer.android.com/reference/android/R.attr#screenOrientation).
- setPosition: Can be used to set the Position of an overlay window in screen pixels.
  - Parameters:
    - aid: The id of the Activity.
    - x
    - y
- getConfiguration: Get the current configuration of an Activity.
  - Returns:
    - dark_mode: boolean. Only on Android 10+.
    - country: The country as a 2-letter string.
    - language: The language as a 2-letter string.
    - orientation: The screen orientation, either "landscape" or "portrait".
    - keyboardHidden: boolean whether a keyboard is currently available.
    - screenwidth: The current window width in dp.
    - screenheight: The current window height in dp.
    - fontscale: The current font scale value as a floating point number.
    - density: The display density as a float, such that screenwidth * density = screenwidth_in_px.
  - Parameters:
    - aid: The id of the Activity.
- turnScreenOn: Turns the screen on. Note that this does not unlock the lockscreen.
- isLocked: Check if the device is locked. Returns true if the device is locked, false if not.
  - Returns:
    - locked: Whether the device is locked.
- requestUnlock: If the lockscreen isn't protected by a PIN, pattern or password, unlocks it immediately. If it is, brings up the UI to let the user unlock it. Only available on Android 8.0 and higher, on lower versions it's a no-op.
  - Parameters:
    - aid: The Activity id of the Activity that wants to unlock the screen.
- hideSoftKeyboard: Forces to soft keyboard to hide.
  - Parameters:
    - aid: The Activity id of the Activity that wants to hide the soft keyboard.
- interceptBackButton: 
  - Parameters:
    - aid: The Activity id of the Activity that wants intercept back button presses.
    - intercept: A boolean, whether to intercept back button presses and send an event or to use the default action of finishing the Activity.
- getVersion: Gets the version code of the plugin app. Can be used for feature detection or to prompt the user to update.


Layout and View control:  
These methods create and Manipulate [Views](https://developer.android.com/reference/android/view/View), [Layouts](https://developer.android.com/guide/topics/ui/declaring-layout) and the Layout hierarchy.  
Due to Android limitations, methods that return a value fail when the Activity isn't visible and up to 100 methods that don't return a value will be queued up to run when the Activity is visible again.
- create*: Creates a View and places it in the Layout hierarchy. It returns the View id or -1 in case of an error. The main cause of an error is a stopped Activity, as it's state isa already saved and can't be changed until it is resumed.
  - The following Views and Layouts are supported:
    - [LinearLayout](https://developer.android.com/guide/topics/ui/layout/linear)
    - [FrameLayout](https://developer.android.com/reference/android/widget/FrameLayout)
    - [SwipeRefreshLayout](https://developer.android.com/reference/androidx/swiperefreshlayout/widget/SwipeRefreshLayout)
    - [TextView](https://developer.android.com/reference/android/widget/TextView)
    - [EditText](https://developer.android.com/reference/android/widget/EditText)
    - [Button](https://developer.android.com/reference/android/widget/Button)
    - [ImageView](https://developer.android.com/reference/android/widget/ImageView)
    - [Space](https://developer.android.com/reference/android/widget/Space)
    - [NestedScrollView](https://developer.android.com/reference/androidx/core/widget/NestedScrollView)
    - [HorizontalScrollView](https://developer.android.com/reference/android/widget/HorizontalScrollView)
    - [RadioButton](https://developer.android.com/guide/topics/ui/controls/radiobutton)
    - [RadioGroup](https://developer.android.com/reference/android/widget/RadioGroup)
    - [Checkbox](https://developer.android.com/guide/topics/ui/controls/checkbox)
    - [ToggleButton](https://developer.android.com/guide/topics/ui/controls/togglebutton)
    - [Switch](https://developer.android.com/reference/android/widget/Switch.html)
    - [Spinner](https://developer.android.com/guide/topics/ui/controls/spinner)
    - [ProgressBar](https://developer.android.com/reference/android/widget/ProgressBar)
    - [TabLayout](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout)
    - [GridLayout](https://developer.android.com/reference/android/widget/GridLayout)
    - [WebView](https://developer.android.com/reference/android/webkit/WebView)
  - The following Views will be supported in the future:
    - [RelativeLayout](https://developer.android.com/guide/topics/ui/layout/relative)
    - [AutocompleteTextView](https://developer.android.com/reference/android/widget/AutoCompleteTextView)
    - [ImageButton](https://developer.android.com/reference/android/widget/ImageButton)
  - The following Views may be supported in the future:
    - [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)
    - [ViewPager2](https://developer.android.com/guide/navigation/navigation-swipe-view-2)
  - Parameters:
    - parent: The View id of the parent in the Layout hierarchy. if not specified, this will replace the root of the hierarchy and delete all existing views.
    - aid: The id of the Activity in which to create the View.
    - text: For Button, TextView and EditText, this is the initial Text.
    - selectableText: For TextViews, this specifies whether the text can be selected. Default is false.
    - clickableLinks: For TextViews, this specifies whether links can be clicked or not. Default is false.
    - vertical: For LinearLayout, this specifies if the Layout is vertical or horizontal. If not specified, vertical is assumed.
    - snapping: NestedScrollView and HorizontalScrollView snap to the nearest item if this is set to true. Default is false.
    - fillviewport: Makes the child of a HorizontalScrollView or a NestedScrollView automatically expand to the ScrollView size. Default is false.
    - nobar: Hides the scroll bar for HorizontalScrollView and NestedScrollView. Default is false.
    - checked: Whether a RadioButton, CheckBox, Switch or ToggleButton should be checked. Defaults to false.
    - singleline: Whether an EditText should enable multiple lines to be entered.
    - line: Whether the line below an EditText should be shown.
    - blockinput: Disables adding the typed key automatically to a EditText and instead sends a key event.
    - type: For EditText this specifies the [input type](https://developer.android.com/reference/android/widget/TextView#attr_android:inputType) : can be one of "text", "textMultiLine", "phone", "date", "time", "datetime", "number", "numberDecimal", "numberPassword", "numberSigned", "numberDecimalSigned", "textEmailAddress", "textPassword". "text" is the default. Specifying singleline as true sets this to "text".
    - rows, cols: Row and column count for GridLayout
    - visibility: Visibility of the View. See setVisibility. If not present, visible is assumed.
    - allcaps: Use this when creating a button to make all text automatically all caps (using small caps if possible).
- showCursor: Sets whether or not a cursor is shown in the EditText.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - show: A boolean.
- setLinearLayoutParams: Sets the LinearLayout parameters for a View in a LinearLayout.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - weight: Sets the Layout weight.
    - position: The index of the element. If null, the position is kept.
- setGridLayoutParams: Sets the GridLayout parameters for a View in a GridLayout.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - row, col: The row and column of the View.
    - rowsize, colsize: The amount of cells in a row/column the View takes up.
    - alignmentrow, alignmentcol: "top", "bottom", "left", "right", "center", "baseline" or "fill". Default is "center".
- setViewLocation: Sets position of a View.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - x, y: The new position.
    - dp: the position is in dp if true, else in pixels.
    - top: If true, the View will be on top of all sibling Views.
- setRelativeLayoutParams: Sets the RelativeLayout parameters for a View in a RelativeLayout.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
- setVisibility: Sets the visibility of a Vew.
  - Parameters:
    - vis: 0 = gone, 1 = hidden, 2 = visible. While hidden, views are not visible but still take up space in the layout. Gone views do not take up layout space.
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
- set(Width/Height): Sets the width/height of the view.
  - Parameters:
    - width/height: The desired width/height. Can be a value in [dp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension), or "MATCH_PARENT" to match the size of the parent layout or "WRAP_CONTENT" to go as big as the view needs to be to display all its content.
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - px: If true, the measurement is in pixels instead of dp.
- getDimensions: Gets the current with and height of a View in pixels.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
- deleteView: Deletes a View and its children from the Layout hierarchy.
  - Parameters:
    - id: The id of the View to delete.
    - aid: The id of the Activity the View is in.
- deleteChildren: Deletes the children of a View from the Layout hierarchy.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
- setMargin: Sets the margin of a View.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - margin: The margin value as an integer in [dp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension).
    - dir: can be "top", "bottom", "left", "right" to set the margin for one of those directions. If not specified. the margin is set for all directions.
- setPadding: Sets the padding of a View.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - padding: The padding value as an integer in [dp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension)
    - dir: can be "top", "bottom", "left", "right" to set the padding for one of those directions. If not specified. the padding is set for all directions.
- setBackgroundColor: Sets the background color of a View.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - color: The color in the same format as for setTheme.
- setTextColor: Sets the text color of a View.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - color: The color in the same format as for setTheme.
- setProgress: Sets the Progress of a ProgressBar.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - progress: The progress value as an integer in the range of 0 to 100.
- setRefreshing: Sets whether a SwipeRefreshLayout is refreshing.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - refresh: The refreshing value as a boolean.
- setText: Sets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - text: The text.
- setGravity: Sets the gravity of the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - horizontal: 0: left/top, 1: center, 2: right/bottom. Right and left are inverted for right-to-left layouts.
    - vertical: Same values as for horizontal.
- setTextSize: Sets the text size.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - size: The text size in [sp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension).
- getText: gets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
- setChecked: Sets a RadioButton, CheckBox, Switch or ToggleButton to checked or unchecked explicitly.
  - Parameters:
    - id: The View id.
    - aid: The id of the Activity the View is in.
    - checked: Whether a RadioButton, CheckBox, Switch or ToggleButton should be checked.
- requestFocus: Focuses a View and opens the soft keyboard if the View has Keyboard input.
    - id: The View id.
    - aid: The id of the Activity the View is in.
    - forcesoft: Forces the soft keyboard to show.
- getScrollPosition: Gets the x and y scroll position of an NestedScrollView or HorizontalScrollView.
  - Returns:
    - The x and y scroll positions.
- setScrollPosition: Sets the x and y scroll position of an NestedScrollView or HorizontalScrollView.
  - Parameters:
    - x
    - y
    - soft: if true, scrolls with an animation instead of jumping to the destination.
- setList: Set the list of a Spinner or TabLayout.
  - Parameters:
    - id: The View id of a Spinner or TabLayout.
    - aid: The id of the Activity the View is in.
    - list: An array containing the available Spinner options / TabLayout tab titles as strings.
- setImage: Sets the image for an ImageView.
  - Parameters:
    - id: The View id of a ImageView.
    - aid: The id of the Activity the View is in.
    - img: The image data in PNG or JPEG format.
- addBuffer: Adds a buffer to be used for ImageViews. Returns to id of the generated buffer. Returns -1 in case of an error. This should be a file descriptor to a shared memory file. You can then write to the shared memory and use the image in the plugin without having to transmit it. When successful, it transfers a file descriptor via SCM_RIGHTS. That file descriptor can then be mapped.
  - Parameters:
    - format: the Image buffer format. Supported: "ARGB8888". The order in memory will be rgba with one byte each.
    - w: The width of the buffer.
    - h: The height of the buffer.
- deleteBuffer: Deletes a shared buffer. You have to do this when you aren't using a buffer anymore (eg. it is to small and you want a larger one), because Buffers consume a lot of memory. Only call this once no ImageView uses this buffer anymore, by setting another buffer, an image or removing the ImageView.
  - Parameters:
    - bid: id of the buffer.
- blitBuffer: Copies the content from the Shared memory buffer into the buffer.
  - Parameters:
    - bid: id of the buffer.
- setBuffer: Sets the ImageView to have a buffer as a source.
  - Parameters:
    - id: The View id of a ImageView.
    - aid: The id of the Activity the View is in.
    - bid: id of the buffer.
- refreshImageView: This should be called after blitting an ImageView's buffer to show the new contents.
  - Parameters:
    - id: The View id of a ImageView.
    - aid: The id of the Activity the View is in.
- selectTab: Selects a Tab in a TabLayout. The corresponding itemselected event is also send.
  - Parameters:
    - id: The View id of a TabLayout.
    - aid: The id of the Activity the View is in.
    - tab: The index of the tab to select, starting at 0.
- setClickable: Sets whether a View can be clicked by the user (if yes, emits a sound and animation when clicked and sends a click event (if click events are enabled))).
  - Parameters:
    - id: The View id of a View.
    - aid: The id of the Activity the View is in.
    - clickable: Whether the View should be clickable.




WebView:  
A WebView can be used as a browser integrated in an app. You can control the size and placement with Layouts like any other View, and even hide the browser UI like the address bar.  
However historically WebView had many critical vulnerabilities. The default settings try to be as secure as possible, but you should still make sure to NEVER load untrusted Data into a WebView.  
E.g. running a local webserver with node.js and displaying it in a WebView with Termux:GUI is fine, but you should not allow users to navigate away from your webserver.  
To help with security, the default settings are:
- Javascript execution is not allowed. You can enable it, but this will result in a blocking dialog asking the user for confirmation, and can be denied.
- File access via URIs is not allowed.
- content:// URIs are not allowed.
- Navigation is blocked. You can only set the page content through methods in Termux:GUI.

Methods:
- allowJavascript: Asks the user for permission to run Javascript in the WebView, if it isn't enabled globally in the options. Returns whether Javascript is enabled after the call.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - allow: Whether to allow or disallow Javascript.
- allowContentURI: Sets whether loading content:// URIs is allowed in the WebView. This can be used to open data from other apps in the WebView.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - allow: Whether to allow or disallow content:// URIs.
- setData: Sets the document displayed in the WebView.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - doc: A string containing the HTML document.
- loadURI: Loads a document from the provided URI.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - uri: A URI string
- allowNavigation: Allows Javascript and the user to navigate. The recommended solution is to catch the navigation events, check the URL and call loadURI, but this needs more work from the client. Currently only works on Android 8.0 and higher.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - allow: Whether to allow or disallow navigation in the WebView.
- goBack: Navigates back in the history.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
- goForward: Navigates forward in the history.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
- evaluateJS: Runs Javascript code in the WebView.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the WebView.
    - code: The Javascript code.




Event control:  
- send*Event: Sets whether events are send or not for a specific View.
  - Supported Events:
    - Click
    - LongClick
    - FocusChange
    - Touch
    - Gesture: not implemented yet. You can detect gestures like pinch yourself with touch events.
    - Text: emits an event every time the text of the View is changed. This listener cannot be unset.
  - Parameters:
    - id: The View id.
    - aid: The id of the Activity the View is in.
    - send: Whether to send the event ot not, a boolean.


RemoteViews:  
RemoteViews are used for homescreen widgets and custom notifications.  
Due to Android limitations, you can only have a specific number of distinct Views of each type per RemoteViews.  
- createRemoteLayout: Creates a new remote Layout. Returns the id for the remote Layout
- deleteRemoteLayout: Deletes a remote Layout.
  - params:
    - rid: The id of the remote Layout to delete.
- addRemoteFrameLayout: creates a FrameLayout in a remote Layout. Returns the View id in the Layout.
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
- <s>addRemoteLinearLayout: creates a LinearLayout in a remote Layout. Returns the View id in the Layout.</s> Doesn't work currently.<s>
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
    - vertical: The same as a normal LinearLayout</s>
- addRemoteTextView: creates a TextView in a remote Layout. Returns the View id in the Layout.
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
- addRemoteButton: creates a Button in a remote Layout. Returns the View id in the Layout.
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
- addRemoteImageView: creates an ImageView in a remote Layout. Returns the View id in the Layout.
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
- addRemoteProgressBar: creates a ProgressBar in a remote Layout. Returns the View id in the Layout.
  - params:
    - rid: id of the remote Layout.
    - parent: id of the parent View. If not specified, it is added to the remote Layout itself.
- setRemoteBackgroundColor: Sets the background color for a View in a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - color: A color value in the format of setTheme.
- setRemoteProgressBar: Sets values for a ProgressBar in a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - max: The 100% value for the ProgressBar.
    - progress: The current progress value for the ProgressBar.
- setRemoteText: Sets the text of a remote View.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - text: The text of the View.
- setRemoteTextSize: Sets the text size of a remote View.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - size: The text size.
    - px: If true size is a value in pixels, otherwise in dip.
- setRemoteTextColor: Sets the Text color for a View in a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - color: A color value in the format of setTheme.
- setRemoteVisibility: Sets the visibility for a View in a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - vis: 0 = gone, 1 = hidden, 2 = visible. While hidden, views are not visible but still take up space in the layout. Gone views do not take up layout space.
- setRemotePadding: Sets the padding in pixels for a View in a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - left, top, right, bottom: The padding values for the directions.
- setRemoteImage: Sets the image of a remote ImageView.
  - params:
    - rid: id of the remote Layout.
    - id: id of the View in the remote Layout.
    - img: The image data in PNG or JPEG format.
- setWidgetLayout: Sets the layout of a Widget to a remote Layout.
  - params:
    - rid: id of the remote Layout.
    - wid: id of the Widget.



Custom Notifications:
- createNotificationChannel: On Android 8.0 and higher this creates a NotificationChannel, if one with the given id doesn't exist. 
  - params:
    - id: The id of the created notification channel. Notification channels are shared between all clients using Termux:GUI, so choose a unique name.
    - importance: 0: min, shows the icon in the tray, 1: low, shows the notification, 2: default, can make noise, 3: high, intrusive, 4: max, very intrusive
    - name: The displayed name for the channel in the Android notification settings.
- createNotification: Creates a notification. Returns the notification id used for further calls. layout, expandedLayout and hudLayout are incompatible with title, content, largeImage and largeText.
  - params:
    - id: The id of the notification to update. If not specified, generates a new id.
    - ongoing: If true, the notification cannot be dismissed by the user, but the notification is automatically dismissed when you close the connection to the plugin.
    - layout: The id of the remote Layout to use.
    - expandedLayout: The id of the remote Layout to use when the notification has been expanded. Optional.
    - hudLayout: The id of the remote Layout to use when the notification is shown as a head-up notification Optional.
    - title: The notification title.
    - content: The notification content text.
    - largeImage: A large image to display in the expanded view. You can only set either largeImage or largeText.
    - largeText: A large block of text to display in the expanded view. HTML formatting is supported. You can only set either largeImage or largeText.
    - largeImageAsThumbnail: If true, the largeImage is shown as a thumbnail in the collapsed view.
    - icon: An image for the Notification in PNG or JPEG. Defaults to the Termux:GUI app icon if left empty.
    - channel: The notification channel id.
    - importance: The same as for createNotificationChannel. This is the importance used on Android versions lower than 8.0.
    - alertOnce: If this call is used to update a notification, don't alert the user again.
    - showTimestamp: Shows the timestamp of the notification.
    - timestamp: Optional. The timestamp to use in form of milliseconds since start of the epoch.
    - actions: Optional. An array of strings with the names of actions. Pressing actions will generate a notificationaction event
- cancelNotification: Cancels a notification.
  - params:
    - id: The id of the notification to cancel.






## Events

Once you have opened an Activity and placed all Views and configured it, like mit GUI applications you have to wait for user input.  
Events arrive on the event socket.  
Events that are enabled by default:
- click for Buttons, Checkboxes, Switches, ToggleButtons
- refresh for SwipeRefreshLayout
- selected for RadioGroup
- itemselected for Spinner/TabLayout


Event types:  
- [Input Events](https://developer.android.com/guide/topics/ui/ui-events#EventListeners) :
  - values for all:
    - id: The id of the View that fired the event.
    - aid: The id of the Activity the View is in.
  - click
    - Additional values for ImageView: x and y position in the view
    - Additional values for CheckBox, Switch and ToggleButton: set: whether the View is set or not
  - longClick
  - focusChange
    - Additional value: focus: whether or not the View now has focus.
  - touch
  - refresh: Refresh triggered in a SwipeRefreshLayout
  - selected: A RadioButton in a RadioButtonGroup has been selected
    - Additional values: selected: The id of the now selected RadioButton
  - itemselected: When a Item is selected in a Spinner / a tab clicked in TabLayout
    - Additional values: selected: The item as a String, or null if no item was selected, or the tab id that was selected for TabLayout.
  - input: Send from EditText where you set the input to blocked
  - cut: Send from EditText where you set the input to blocked
  - paste: Send from EditText where you set the input to blocked
  - text: Send when the Text of the View changed, even when the text was changed with setText.
    - Additional values: text: The new text of the view
  - back: Send when you set an Activity to intercept the back button press and the back button is pressed. No id, since it originates from the Activity.
  - webviewNavigation: Whenever JS or the user tries to navigate.
    - value:
      - url: The URL that will/would be loaded
  - webviewHTTPError: When the WebView receives an HTTP error code
    - value:
      - url: The URL the error came from
      - code: The error code
  - webviewError: When the WebView can't load something, but it's not an HTTP error.
    - value:
      - url: The URL the error came from
  - webviewDestroyed: When Android kills the WebView renderer process, the WebView is removed and this event is fired.
  - webviewProgress: The progress of loading the current site.
    - value:
      - progress: The percentage of progress.
  - webviewConsoleMessage: When JS wants to print a message in the console.
    - value:
      - msg: The message.
- [Activity Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle) :
  - values for all:
    - aid: The id of the Activity.
  - create
  - start
  - resume
  - pause
    - value: finish: whether or not the Activity is finishing
  - stop
    - value: finish: whether or not the Activity is finishing
  - destroy:
    - value: finish: whether or not the Activity is finishing. Only for destroy this is guaranteed to be accurate. The previous events may not report that. So if you want to save state when the Activity is destroyed, request that state when it is stopped instead.
  - config: The current configuration changed. The additional values are the same as for getConfiguration.
- Custom events:
  - UserLeaveHint: Gets fired when the user leaves an Activity. Can be used to then make the Activity go into pip mode.
  - pipchanged: Gets fired when the Activity enters or exits pip mode.
    - value: Whether the Activity is now in pip mode or not.
  - airplane: Gets fired when the Airplane mode is changed.
    - value: Boolean of the new Airplane mode.
  - locale: Gets fired when the user changes the locale:
    - value: The language code as a ISO 639 string.
  - screen_on: Gets fired when the screen is turned on.
  - screen_off: Gets fired when the screen is turned off.
  - timezone: Gets fired when the time zone changes.
  - overlayTouch: Like touch, but is dispatched for every touch in an overlay window. The coordinates are the absolute screen coordinates
  - notification: Generated when the user taps a notification.
    - value: id: The id of the notification
  - notificationaction: Generated when the user clicks a notification action.
    - value:
      - id: The id of the notification
      - action: The index of the action, starting at 0.
  - remoteclick: Like click, but for buttons in RemoteViews.
    - value:
      - rid: Id of the remote layout.
      - id: id of the remote Button.

#### Touch events

Touch events are more complex, because multitouch is supported.  
The additional values are:
- action: one of "up", "down", "pointer_up", "pointer_down", "cancel", "move", corresponding to [MotionEvent values](https://developer.android.com/reference/android/view/MotionEvent#constants_1) for ACTION_DOWN etc.
- index: for "pointer_up" and "pointer_down" this is the index of the pointer removed/added.
- time: The time of the event in milliseconds since boot excluding sleep. Use this when checking for gestures or other time-sensitive things.
- pointers: An array of arrays of pointer data objects. The first dimension is for grouping multiple move events together. The second dimension is for the pointer positions in each event. The elements in the array are:
  - x, y: The coordinates of the pointer inside the View (not in the window). For ImageView, these are the coordinates of the pixel in the displayed image or buffer, so you don't need to convert the position yourself.
  - id: The pointer id. This stays consistent for each pointer in the frame between "up" and "down" events

## JSON protocol

The Messages are JSON objects with the "method" value being the name of the method and "params" being an optional JSON object with the parameters.  
The parameter keys are as described above.  
Binary data (like images) has to be transmitted base64 encoded as a JSON string.  
If a method returns only one value, it is returned as a single JSON value.  
If multiple values are returned, they are placed in a JSON array.  
Events are a JSON object with the field "type" denoting the event type and the field "value" that depends on the event type.  


## Binary Protocol

The binary protocol is/will be implemented with protocol buffers.  
See app/src/proto for the definitions and documentation.  
The general flow after establishing a connection is this:
- The client sends a Method message with the *Request in the oneof set for the method it would like to call.
- The plugin sends back the corresponding *Response message.

The only exception to this is the addBuffer method:  
Instead of a response message the plugin sends a 32 bit big-endian signed integer and after that one byte with a file descriptor as ancillary data.
If the integer is negative, a buffer could not be created and there will be no byte with a file descriptor.


The events are a stream of Event messages with the actual Event inside the event oneof.

All protobuf messages are read and written delimited.



