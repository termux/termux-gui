# Termux:GUI

<img src="https://img.shields.io/github/v/release/termux/termux-gui?include_prereleases"/>
<img src="https://img.shields.io/f-droid/v/com.termux.gui"/>


This is a plugin for [Termux](https://github.com/termux/termux-app) that enables command line programs to use the native android GUI.  
  
In the examples directory you can find demo videos, sample code is provided in the tutorials of the official language bindings.

[There are also prepackaged programs you can use](https://github.com/tareksander/termux-gui-package).

[Installation notes](https://github.com/termux/termux-app#installation)  
There is currently no release on f-droid, so the only method to install this plugin is to use the apk from the [releases](https://github.com/termux/termux-gui/releases) and the [Termux apk from Github Actions](https://github.com/termux/termux-app/actions).  
Releases on f-droid will be provided as soon as possible. ~~When there is a release, the f-droid badge at the top will show a version number.~~ The f-droid builds currently don't work, Github builds are the only option for now.  
[See this comment for links to install Termux and its plugins from Github](https://github.com/tareksander/termux-gui-python-bindings/issues/1#issuecomment-983797979).  

[Protocol.md](Protocol.md) describes the Protocol used and the available functions you can use.  
[GUIProt0.proto](app/src/main/proto/GUIProt0.proto) contains the documentation of the newer Protobuf-based protocol with more features.  
If you want to use overlay windows or be able to open windows from the background, go into the app settings for Termux:GUI, open the advanced section and enable "Display over other apps".  

### Features

- buttons, switches, toggles, checkboxes, text fields, scrolling, LinearLayout
- custom notifications
- custom widgets
- shared image buffers
- GLES2 acceleration
- WebView
- Dialogs
- Lockscreen Activities
- Wake-lock
- ...and more



### Comparison with native apps

| Native app                                                | With Termux:GUI                                                             |
|-----------------------------------------------------------|-----------------------------------------------------------------------------|
| Has to be installed                                       | Program can be run in Termux                                                |
| Full access to the Android API                            | Access to the Android API through Termux:GUI and Termux:API                 |
| Limited to C, C++, Kotlin and Java for native development | Any programming language can be used, prebuilt library for python available |
|                                                           | Lower performance caused by IPC                                             |
| Accessing files in Termux only possible via SAF           | Direct access to files in Termux                                            |
| Has to be started with `am` from Termux                   | Can be started like any other program in Termux                             |
|                                                           | Can receive command line arguments and output back to the Terminal          |


## Language Bindings

### Official

- [Python](https://github.com/tareksander/termux-gui-python-bindings)
- [C/C++](https://github.com/tareksander/termux-gui-c-bindings)
- [Bash](https://github.com/tareksander/termux-gui-bash)

### Community

Not maintained by the plugin maintainer:

- [Rust](https://github.com/sweetkitty13/tgui-rs)

## Using the plugin

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
  
With these fundamentals you can go ahead and use this plugin.  If you need more sophisticated Views or Layouts look into Protocol.md for what's available.




