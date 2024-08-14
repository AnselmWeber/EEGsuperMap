# EEGsuperMap

## Installation
Install SuperCollider:

Copy the superMapExtensions Folder into your SuperCollider Extensions Folder in your library:

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
Contents > Resources > SCClassLibrary > JITLib > ProxySpace > Replace wrapForNodeProxy.sc

### In SuperCollider:
```supercollider
Quarks.gui;
```
Search for OpenBCI + JitlibExtensions and add it.
Then install OpenBCI port for SuperCollider
```supercollider
//install
Quarks.fetchDirectory
Quarks.install("OpenBCI-SuperCollider")
//recompile
OpenBCI.openHelpFile
```

### Recompile SC Class Library
Hit ⌘ + ⇧ + L or go to Language > Recompile Class Library

