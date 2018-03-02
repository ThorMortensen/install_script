# Universal Linux Environment Installer
---
## Description
This is a environment installer for Linux that will setup a clean installation of Linux ready to use. It consists of a series of smaller install scripts and GUI to give an overview over the different components to install and enable the user to choose the components to install.
As of now it is not completely automated and actions may still be required from the user under the various installations. Please finish up all external installation procedures before closing the installer as it will cleanup all downloaded installation files when it quits.


#### Supported Distros
* Ubuntu
* Open SUSE (work in  progress)


## How To Use

It is assumed that everything is installed at default locations

## Customization

### How It Works

The GUI will scan all files in the script folder. The file name (file extensions are ignored) will be shown as a install item in the list. I.e “File Name.sh” will be shown as “File Name”. No special characters are allowed (‘“/,). Dots are considered as file extensions delimiter and everything after the dot is not considered part of the item name.

The GUI will scan each file for a description. A description is defined as:

```
if [ "$1" == "-h" ]; then
  echo "This is a description"
  exit 0
fi
```

The description must be the first thing in the script after the shebang (`#!/bin/bash`) else it will be ignored.

At the moment it is not possible to define dependencies but it may be included at a later point.


### Things To Consider (quirks and novelties)

When a desciption is found the GUI will call the script with the `-h` argument as the first parameter. Please consider what you put in the descritpin and remember to exit else the script will be run before the user has a chance to choose it.

### Adding New Scripts

Each script should be a selfcointing recepi to install one item.

To add a new script just make a new file in the script folder and give it a meaningfull name. (File extentions are ignored by the GUI). Add the shebang on the first line:
```
#!/bin/bash
```

(As of the time of writing to get the most reliable scripts use bash)

** The script will be called with the following parameters **
* `$1` ** ~> Aboslute path to the script **
* `$2` ** ~> User name **
* `$3` ** ~> User password **


Add a discription that will be shown as a tooltip in the GUI or be printed when the script is called from the command line with the `-h` parameter.

```
if [ "$1" == "-h" ]; then
  echo "This is a description"
  exit 0
fi
```

A number of helper functions are available in the `resources/worker_scripts/helpers` script. To use them add this to your script:

```
. resources/worker_scripts/helpers
```

It is recomended to use the helper funtions as thay have been tested to work with the GUI and will call the funtions with the required arguments and place files at the correct locations.

If you need to download or make temporary files place them in the `resources/downloads/` folder as it will be cleaned every time the GUI quits. The path to the folder is defied in the helpers script as
```
$DOWNLOAD_PATH
```

But remember files here will be deleted :-)
