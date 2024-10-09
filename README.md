# EEGsuperMap

## Install
Install [SuperCollider](https://supercollider.github.io/)

Copy the superMapExtensions folder from src/sc into your SuperCollider Extensions folder in your library:

```
/Users/*yourusername*/Library/Application Support/SuperCollider/Extensions
```
You can also find it in SuperCollider with

```supercollider
Platform.userExtensionDir
```

The folder might not exist, so you may need to create it yourself. You can do this in your operating system's file
explorer or from within SuperCollider by evaluating:

```supercollider
File.mkdir(Platform.userExtensionDir)
```

Go to your SuperCollider App. Alt/right + click > show package contetes. Then navigate to:
Contents > Resources > SCClassLibrary > JITLib > ProxySpace
There replace wrapForNodeProxy.sc with the one in the src/sc folder from this repo.

### In SuperCollider:

Install OpenBCI port for SuperCollider

```supercollider
//install
Quarks.fetchDirectory
Quarks.install("OpenBCI-SuperCollider")
//recompile
OpenBCI.openHelpFile
```

Install JITLibExtensions:
```supercollider
"JITLibExtensions".include;
```
Sometimes this doesn't work, which shows when recompiling the class library (see next step).

I that is the case just copy the JITLibExtensions from the install folder to:

```
/Users/*yourusername*/Library/Application Support/SuperCollider/downloaded-quarks
```

### Recompile SC Class Library
Hit ⌘ + ⇧ + L or go to Language > Recompile Class Library


## Start Mapping
Go to src/sc

Open band_panner_app.scd and evaluate

Then try it out with simple_synths.scd or with your own synths!




