# lish
A practical Lisp for Bash

## Description
Lish pretends to be a clojure like code interpreter that runs on bash. It initially has been forked from Fleck project using commit 68ca1cdc.

For more information about Fleck, you can visit https://github.com/chr15m/flk

## Installation steps

1. Clone Lish repository.
```
git clone https://github.com/corsanhub/lish.git
```

2. Go into the cloned project
```
cd lish
```

3. Start coding with an example. Create a new "Hello World" example:
```
$ ./create hello
```

It will create a file**hello.clj** wich contains the code shown below:
```
#!/usr/bin/env lish
;(ns hello)

(defn say-hello [name]
  (println (str "Hello there " name "!")))

(say-hello "K'ptzin")
```

4. That's it! execute the script.

Using runner script
```
./run hello.clj
```
or

Adding **bin** directory to the $PATH environment variable
```
export PATH=$PWD/bin:$PATH
```
* Note: Add previous line to *.bashrc* file if needed for upcomming Bash sessions


Then script can be executed right away, just like this:
```
./hello.clj
```

## Examples

### Image transformation

For converting an image from 2D into an isometric at 30 degres (Angle can be adjusted XD) just for draw.io 3D perspective,
from this:

![Input Image](https://github.com/corsanhub/lish/blob/develop/images/input.png)

to this

![Output Image](https://github.com/corsanhub/lish/blob/develop/images/output.png)

Execute **imgot** script as shown:

```
./run imgot images/input.png images/output.png
```
