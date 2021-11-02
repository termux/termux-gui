# Protocol

If you don't want to implement a custom library for using the protocol, you can skip to the [protocol methods](#methods)<!-- @IGNORE PREVIOUS: anchor -->

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
- newActivity: Launches a new Activity and return the activity id.
  - Parameters:
    - tid: the task in which the Activity should be started. If not specified, a new Task is created and the id is returned after the Activity id.
    - flags: Flags to set when launching the Activity via the Intent.
    - isDialog: boolean value. If true, the Activity will be launched as a dialog.
- finishActivity: Closes an Activity.
  - Parameters:
     - aid: The id of the Activity to close.
- finishTask: Closes a Task including all activities in it.
  - Parameters:
    - tid: The id of the Task to close.
- bringTaskToFront: Shows a Task to the user if it wasn't already.
  - Parameters:
    - tid: The id of the Task to show.
- setTheme: Sets the theme for the activity. It only applies to newly created Views, so this should be set before any Views are added.The color has to be specified as an RGBA888 integer (in hex literals 0xaabbggrr).
  - Parameters:
    - aid: The Activity id.
    - statusBarColor
    - colorPrimary
    - windowBackground
    - textColor
    - colorAccent
- setTaskIcon: Sets the Icon of the Task. The background color of the icon will be the current primary color.
  - Parameters:
    - aid: The id of an Activity in the Task you want to set the icon.
    - img: The image data in PNG or JPEG format.


Layout and View control:  
These methods create and Manipulate [Views](https://developer.android.com/reference/android/view/View), [Layouts](https://developer.android.com/guide/topics/ui/declaring-layout) and the Layout hierarchy.
- create*: Creates a View and places it in the Layout hierarchy. It returns the View id.
  - The following Views and Layouts are supported:
    - [LinearLayout](https://developer.android.com/guide/topics/ui/layout/linear)
    - [RelativeLayout](https://developer.android.com/guide/topics/ui/layout/relative)
    - [TextView](https://developer.android.com/reference/android/widget/TextView)
    - [EditText](https://developer.android.com/reference/android/widget/EditText)
    - [Button](https://developer.android.com/reference/android/widget/Button)
    - [Canvas](https://developer.android.com/reference/android/graphics/Canvas)
    - [Space](https://developer.android.com/reference/android/widget/Space)
    - [NestedScrollView](https://developer.android.com/reference/androidx/core/widget/NestedScrollView)
    - [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)
  - Parameters:
    - parent: The View id of the parent in the Layout hierarchy. if not specified, this will replace the root of the hierarchy and delete all existing views.
    - aid: The id of the Activity in which to create the View.
    - text: For Button, TextView and EditText, this is the initial Text.
    - vertical: For LinearLayout, this specifies if the Layout is vertical or horizontal. If not specified, vertical is assumed.
- deleteView: Deletes a View and its children from the Layout hierarchy.
  - Parameters:
    - id: The id of the View to delete.
    - aid: The id of the Activity in which the View is in.
- setMargin: Sets the Margin of a View.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity in which the View is in.
    - margin: The margin value as an integer in [dp](https://developer.android.com/guide/topics/resources/more-resources.html#Dimension)
    - dir: can be "top", "bottom", "left", "right" to set the margin for one of those directions. If not specified. the margin is set for all directions.
- setLayoutWeight: Sets the Layout weight for Layouts that support it.
  - Parameters:
    - id: The id of the View.
    - aid: The id of the Activity the View is in.
    - weight: The Layout weight.
- setText: Sets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
    - text: The text.
- getText: gets the text of the View.
  - Parameters:
    - id: The View id of a TextView, Button or EditText.
    - aid: The id of the Activity the View is in.
- inflateJSON: Creates a View hierarchy from a [JSON object](https://github.com/flipkart-incubator/proteus). Warning: you must take care to not have duplicate View ids.
  - Parameters:
    - aid: The id of the Activity the View is in.
    - parent: The View id of the parent in the Layout hierarchy. if not specified, this will replace the root of the hierarchy and delete all existing views.



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


## Events

Once you have opened an Activity and placed all Views and configured it, like mit GUI applications you have to wait for user input.  
Events arrive on the event socket.  
Certain events are send automatically, like Activity lifecycle events and click events for buttons.  

Event types:  
- [Input Events](https://developer.android.com/guide/topics/ui/ui-events#EventListeners) :
  - click
  - longClick
  - focusChange
  - key
  - touch
- [Activity Lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle) :
  - start
    - value: null
  - resume
    - value: null
  - pause
    - value: null
  - stop
    - value: null
  - destroy
    - value: whether or not the Activity is finishing
- Custom events:
  - recitem: The plugin needs an item for a RecyclerView
    - value:
      - rec: the RecyclerView View id
      - it: the index of the item required



## JSON protocol

The Messages are JSON objects with the "method" value being the name of the method and "params" being an optional JSON object with the parameters.  
The parameter keys are as described above.  
Binary data (like images) has to be transmitted bas64 encoded as a JSON string.  
If a method returns only one value, it is returned as a single JSON value.  
If multiple values are returned, they are placed in a JSON array.  
Events are a JSON object with the field "type" denoting the event type and the field "value" that depends on the event type.  



