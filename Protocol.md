# Protocol

If you don't want to implement a custom library for using the protocol, you can skip to the [protocol methods](#methods)<!-- @IGNORE PREVIOUS: anchor -->

### WARNING: The protocol is not yet stable and could change anytime, and parts are not implemented yet.


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
- Binary: For Increased throughput and compatibility with low-level languages, a custom binary protocol can be used.

Regardless of Protocol, each message must be preceded by the length of the message (without this length value) as a 4 byte unsigned integer, the same with the return messages from the plugin.
This integer is send big-endian.

## Protocol negotiation

The first byte send specifies the desired protocol type and version:  
- The 4 most significant bits specify the protocol type: 0 indicates the binary protocol, 1 indicates JSON. Currently only the JSON protocol is implemented.
- The 4 least significant bits specify the protocol version. Currently unused, should be set to 0 for compatibility with future versions.

The plugin then responds with a single byte unsigned integer. A response of 0 means the plugin supports the desired protocol type and version. Any other value denotes an error and the plugin will close the connection.

## <a name="methods"></a>Protocol methods

Each message invokes a method on the plugin.  
Methods may return values, the number of return values can vary with the parameters.  
  
Available methods:  
  
Activity and Task control:  
These methods control Android [Activities](https://developer.android.com/reference/android/app/Activity) and [Tasks](https://developer.android.com/guide/components/activities/tasks-and-back-stack).
- newActivity: Launches a new Activity and returns the activity id. In case of an error, -1 is returned.
  - Parameters:
    - tid: the task in which the Activity should be started. If not specified, a new Task is created and the id is returned after the Activity id.
    - One of:
      - dialog: boolean value. If true, the Activity will be launched as a dialog.
      - canceloutside: boolean value, optional. Sets whether the Activity is destroyed when it is displayed as a dialog and the user clicks outside. Default is true.
      - pip: boolean. Whether or not to start the Activity in [Picture-in-Picture mode](https://developer.android.com/guide/topics/ui/picture-in-picture). Default is false. This should only be used to create Activities in a new Task.
      - lockscreen: displays this Activity on the lockscreen.
      - overlay: uses a system overlay window to display above everything else. Overlays are never in a Task and creating one doesn't return a Task id. Overlays don't get Activity lifecycle events.
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
- setTheme: Sets the theme for the activity. It only applies to newly created Views, so this should be set before any Views are added. The color has to be specified as an RGBA888 integer (in hex literals 0xaabbggrr).
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
- toast: Sends a [Toast](https://developer.android.com/guide/topics/ui/notifiers/toasts).
  - Parameters:
    - text: The text to display.
    - long: true if the text should be displayed for longer.
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



Layout and View control:  
These methods create and Manipulate [Views](https://developer.android.com/reference/android/view/View), [Layouts](https://developer.android.com/guide/topics/ui/declaring-layout) and the Layout hierarchy.
- create*: Creates a View and places it in the Layout hierarchy. It returns the View id or -1 in case of an error. The main cause of an error is a stopped Activity, as it's state isa already saved and can't be changed until it is resumed.
  - The following Views and Layouts are supported:
    - [LinearLayout](https://developer.android.com/guide/topics/ui/layout/linear)
    - [RelativeLayout](https://developer.android.com/guide/topics/ui/layout/relative)
    - [FrameLayout](https://developer.android.com/reference/android/widget/FrameLayout)
    - [TextView](https://developer.android.com/reference/android/widget/TextView)
    - [EditText](https://developer.android.com/reference/android/widget/EditText)
    - [Button](https://developer.android.com/reference/android/widget/Button)
    - [ImageView](https://developer.android.com/reference/android/widget/ImageView)
    - [Space](https://developer.android.com/reference/android/widget/Space)
    - [NestedScrollView](https://developer.android.com/reference/androidx/core/widget/NestedScrollView)
    - [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)
    - [AutocompleteTextView](https://developer.android.com/reference/android/widget/AutoCompleteTextView)
    - [RadioButton](https://developer.android.com/guide/topics/ui/controls/radiobutton)
    - [RadioGroup](https://developer.android.com/reference/android/widget/RadioGroup)
    - [Checkbox](https://developer.android.com/guide/topics/ui/controls/checkbox)
    - [ToggleButton](https://developer.android.com/guide/topics/ui/controls/togglebutton)
    - [Switch](https://developer.android.com/reference/android/widget/Switch.html)
    - [ImageButton](https://developer.android.com/reference/android/widget/ImageButton)
    - [Spinner](https://developer.android.com/guide/topics/ui/controls/spinner)
    - [GridLayout](https://developer.android.com/reference/android/widget/GridLayout)
    - [ProgressBar](https://developer.android.com/reference/android/widget/ProgressBar)
    - [ViewPager2](https://developer.android.com/guide/navigation/navigation-swipe-view-2)
    - [TabLayout](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout)
    - [Tab](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout.Tab)
    - [WebView](https://developer.android.com/reference/android/webkit/WebView)
  - Parameters:
    - parent: The View id of the parent in the Layout hierarchy. if not specified, this will replace the root of the hierarchy and delete all existing views.
    - aid: The id of the Activity in which to create the View.
    - text: For Button, TextView and EditText, this is the initial Text.
    - vertical: For LinearLayout, this specifies if the Layout is vertical or horizontal. If not specified, vertical is assumed.
    - checked: Whether a RadioButton, CheckBox, Switch or ToggleButton should be checked. Defaults to false.
    - singleline: Whether an EditText should enable multiple lines to be entered.
    - line: Whether the line below an EditText should be shown.
    - blockinput: Disables adding the typed key automatically to a EditText and instead sends a key event.
    - type: For EditText this specifies the [input type](https://developer.android.com/reference/android/widget/TextView#attr_android:inputType) : can be one of "text", "textMultiLine", "phone", "date", "time", "datetime", "number", "numberDecimal", "numberPassword", "numberSigned", "numberDecimalSigned", "textEmailAddress", "textPassword". "text" is the default. Specifying singleline as true sets this to "text".
- showCursor: Sets whether or not a cursor is shown in the EditText.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - show: A boolean.
- setLinearLayoutParams: Sets the LinearLayout parameters for a View in a LinearLayout.
  - Parameters:
    - parent: The View id of the parent in the Layout hierarchy.
    - aid: The id of the Activity the View is in.
    - id: The id of the View.
    - weight: Sets the Layout weight.
- setRelativeLayoutParams: Sets the RelativeLayout parameters for a View in a RelativeLayout.
  - Parameters:
    - parent: The View id of the parent in the Layout hierarchy.
    - aid: The id of the Activity the View is in.
    - 
- setVisibility: Sets the visibility of a Vew.
  - Parameters:
    - visibility: 0 = gone, 1 = hidden, 2 = visible. While hidden, views are not visible but still take up space in the layout. Gone views do not take up layout space.
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
- set(Width/Height): Sets the width/height of the view.
  - Parameters:
    - width/height: The desired width/height. Can be a value in [dp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension), or "MATCH_PARENT" to match the size of the parent layout or "WRAP_CONTENT" to go as big as the view needs to be to display all its content.
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
- setText: Sets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - text: The text.
- setTextSize: Sets the text size.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - size: The text size in [sp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension).
- getText: gets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
- setChecked: Sets a RadioButton, CheckBox, Switch or ToggleButton to checked or unchecked explicitly. This does not emit an Event.
  - Parameters:
    - id: The View id.
    - aid: The id of the Activity the View is in.
    - checked: Whether a RadioButton, CheckBox, Switch or ToggleButton should be checked.
- setList: Set the list of a Spinner.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - list: An array containing the available Spinner options as strings
- inflateJSON: Creates a View hierarchy from a [JSON object](https://github.com/flipkart-incubator/proteus). Warning: you must take care to not have duplicate View ids.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - parent: The View id of the parent in the Layout hierarchy. if not specified, this will replace the root of the hierarchy and delete all existing views.
- generateIDs: Generates and reserves View ids. Use them to set the ids for the JSON Layout in inflateJSON.
  - Parameters:
    - n: The number of ids to generate
- setImage: Sets the image for an ImageView.
  - Parameters:
    - id: The View id of a ImageView.
    - aid: The id of the Activity the View is in.
    - img: The image data in PNG or JPEG format.
- addBuffer: Adds a buffer to be used for ImageViews. Returns to id of the generated buffer. Returns -1 in case of an error. This should be a file descriptor to a shared memory file. You can then write to the shared memory and use the image in the plugin without having to transmit it. When successful, it transfers a file descriptor via SCM_RIGHTS. That file descriptor can then be mapped. This feature is only supported on Android 8.1+, on earlier versions this always fails.
  - Parameters:
    - format: the Image buffer format. Supported: "ARGB888". The order in memory will be rgba with one byte each.
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


RecyclerViews:
RecyclerViews can be used to display large datasets efficiently.  
All methods that accept a View id can also additionally accept recyclerview as the id of a RecyclerView and recyclerindex as the index of the element in the list.  
The View to act on is then searched in the specified item of the specified RecyclerView.  
These can additionally be used for create* methods to set the item in the RecyclerView.  
These limitations are there so that searching for a View doesn't lead to the whole dataset being searched.




Event control:
- send*Event: Sets whether events are send or not for a specific View.
  - Supported Events:
    - Click
    - LongClick
    - FocusChange
    - Key
    - Touch
  - Parameters:
    - id: The View id.
    - aid: The id of the Activity the View is in.
    - send: Whether to send the event ot not, a boolean.

Widgets:  
App widget support only a subset of Views. See [remote views](https://developer.android.com/reference/android/widget/RemoteViews#public-constructors).  
WARNING: This part will be re-worked due to limitations by Androids widget system.
- bindWidget: Binds a widget to this connection, so you can perform actions on it. Returns 0 on success, another number on failure.
  - Parameters:
    - wid: The widget id.
- blitWidget: updates the widget with the views you created and configured in it.
  - Parameters:
    - wid: The widget id.
- clearWidget: clears the widget representation. This is needed because you can't remove views in a widget, you then have to build it again.
  - Parameters:
    - wid: The widget id.
- createListView: Creates a ListView where content can be placed dynamically

The following methods can also take the parameter wid instead of aid to operate on widgets:
- createLinearLayout: can only be used as the root layout
- createImageView: the image has to be specified in this method and cannot be changed afterwards.
- createButton: The text has to be specified here and cannot be changed afterwards.
- createTextView: The text has to be specified here and cannot be changed afterwards.
- createGridLayout: Works as a collection like ListView
- createProgressBar







## Events

Once you have opened an Activity and placed all Views and configured it, like mit GUI applications you have to wait for user input.  
Events arrive on the event socket.  
Events that are enabled by default:
- click for Buttons, Checkboxes
- 

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
  - key
    - Additional value: key: the key that was pressed.
  - touch
    - Additional values x,y, action: Coordinates in the View, "down", "move" or "up"
  - refresh: Refresh triggered in a SwipeRefreshLayout
  - selected: A RadioButton in a RadioButtonGroup has been selected
    - Additional values: selected: The id of the now selected RadioButton
  - itemselected: When a Item is selected in a Spinner
    - Additional values: selected: The item as a String, or null if no item was selected
  - input: Send from EditText where you set the input to blocked
  - cut: Send from EditText where you set the input to blocked
  - paste: Send from EditText where you set the input to blocked
- [Activity Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle) :
  - values for all:
    - aid: The id of the Activity.
  - create
  - start
  - resume
  - pause
    - value: whether or not the Activity is finishing
  - stop
    - value: whether or not the Activity is finishing
  - destroy:
    - value: finish whether or not the Activity is finishing. Only for destroy this is guaranteed to be accurate. The previous events may not report that. So if you want to save state when the Activity is destroyed, request that state when it is stopped instead.
- Custom events:
  - UserLeaveHint: Gets fired when the user leaves an Activity. Can be used to then make the Activity go into pip mode.
  - pipchanged: Gets fired when the Activity enters or exits pip mode.
    - value: Whether the Activity is now in pip mode or not.
  - overlayTouch: Like touch, but is dispatched for every touch in an overlay window. The coordinates are the absolute screen coordinates
  - overlayScale: 



## JSON protocol

The Messages are JSON objects with the "method" value being the name of the method and "params" being an optional JSON object with the parameters.  
The parameter keys are as described above.  
Binary data (like images) has to be transmitted base64 encoded as a JSON string.  
If a method returns only one value, it is returned as a single JSON value.  
If multiple values are returned, they are placed in a JSON array.  
Events are a JSON object with the field "type" denoting the event type and the field "value" that depends on the event type.  



