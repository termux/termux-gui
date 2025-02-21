# Termux:GUI

[<img src="https://img.shields.io/github/v/release/termux/termux-gui?include_prereleases"/>](https://github.com/termux/termux-gui/releases)
[<img src="https://img.shields.io/f-droid/v/com.termux.gui"/>](https://f-droid.org/de/packages/com.termux.gui/)


This is a plugin for [Termux](https://github.com/termux/termux-app) that enables command line programs to use the native android GUI.  
  
In the examples directory you can find demo videos, sample code is provided in the tutorials of the official language bindings.

[There are also prepackaged programs you can use](https://github.com/tareksander/termux-gui-package). The `termux-gui-package` package contains a collection of small programs.

See the [installation notes](https://github.com/termux/termux-app#installation) on general instructions to install Termux plugins. Clicking on the F-Droid badge above brings you to the F-Droid page for the plugin, the release badge to the GitHub releases.

If you want to use overlay windows or be able to open windows from the background, go into the app settings for Termux:GUI, open the advanced section and enable "Display over other apps".  

For developing applications using the plugin, see [Developing.md](./Developing.md).

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

