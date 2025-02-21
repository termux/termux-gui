## Developing with Termux:GUI

Using this plugin requires a bit of knowledge of the Android GUI system. You can read about it in detail in the [official documentation](https://developer.android.com/guide).  
Relevant documentation is also linked in Protocol.md for more specific subjects.  
Here is a crash-course:

#### Tasks

Tasks are the individual screens you can switch between. Each Task has a stack of Activities, called the back stack, with the top one being visible.

#### Activities

Activities are the individual screens of apps, like a home screen, a settings screen etc.  
When an Activity finishes itself, it is removed from the Activity stack of its Task, showing the underlying Activity.  
An Activity can also launch another Activity in the Task, adding it to the back stack on top of itself.  
To not let the device do unneeded work like drawing Elements that aren't visible, the System informs Activities of certain changes.  
These are the Activity lifecycle events.

#### Activity Lifecycle

The most important lifecycle events for you will be onDestroy, onStart and onStop.
When an Activity is started, it is visible. An Activity is destroyed if the user finishes it (by dismissing it), when it finishes itself or when a configuration change occurs.  
The plugin will tell you if the Activity is finishing when onDestroy is fired, so you know the Activity will be closed.  
Configuration changes are handled for you, so you don't need to worry about onDestroy if the Activity is not finishing.

#### Views

Views are the Elements that are displayed in Activities.  
They are divided into Views and Viewgroups, which can themselves contain Views or Viewgroups.  
That results in a hierarchy of Elements, with one at the top.  
The Viewgroups are Layouts that position the contained Views according to your configuration.

The most used Layout is LinearLayout. It simply displays Views in a horizontal or vertical list, giving each Element the same space by default.  
TextViews can display text.  
EditText can get text input from the user.  
Buttons can be clicked.  
ImageViews can display images.  

### Writing Language Bindings

In case your language doesn't have bindings (listed in the Readme are the known ones), it's easy to create them yourself, especially with the older JSON protocol.
[Protocol.md](./Protocol.md) describes the connection setup, protocol flow and JSON protocol methods. The Protobuf methods are described in the [GUIProt0.proto](./app/src/main/proto/GUIProt0.proto) file (for the current version 0 of the binary protocol).
You can use the Python bindings as a reference for creating JSON-based bindings, they should be quite readable. The C bindings can be used for Protobuf, the internals rely on the Protobuf C++ library.

Another option is to use FFI and rely on the C library.

