# fedit

An in-core editor for Clojure data structures and, particularly, functions.

## Usage

Preliminary, incomplete, alpha quality code. This implements a structure editor in a terminal, 
not a display editor in the tradition of InterLisp's DEdit. I do intend to follow up with a 
display editor, but this is exploratory code.

To edit an arbitrary s-expression:

	(sedit sexpr)

This pretty much works now; it returns an edited copy of the s-expression. Vectors are not handled
intelligently (but could be).

To edit a function definition

	(fedit 'name-of-function)

## Still to do

### Function metadata

Currently, Clojure metadata on a function symbol as follows:
{
	:arglists ([sexpr]), 
	:ns #<Namespace fedit.core>, 
	:name sedit, :column 1, 
	:doc "Edit an S-Expression, and return a modified version of it", 
	:line 74, 
	:file "fedit/core.clj"
}

In order to be able to recover the source of a function which has not yet been committed to the file
system, it would be necessary to store the source s-expression on the metadata. You cannot add new 
metadata to an existing symbol (? check this), but, again, as the package reloader effectively does
so, there must be a way, although it may be dark and mysterious. Obviously if we're smashing and
rebinding the function's compiled form we're doing something dark and mysterious anyway.

### Generating/persisting packages

Editing a function which is in an existing package has problems associated with it. We cannot easily 
save it back to its original file, as that will throw out the line numbering of every other definition 
in the file. Also, critically, files contain textual comments which are not read by the reader, and
consequently would be smashed by overwriting the old definition with the new definition.

Consequently I'm thinking that a revised package manager for Clojure-with-in-core-editing should
generate new packages with names of the form packagename_serial; that when files are edited in core,
the serial number should be incremented to above the highest existing serial number for that package,
and the new package (with the new serial number) should depend on the next-older version of the 
package (obviously, recursively). The function (use 'packagename) should be rewritten so if passed 
a package name without a version number part, it would seek the highest numbered version of the 
specified package available on the path.

At the end of a Clojure session (or, actually, at any stage within a session) the user could issue 
a directive

(persist-edits)

Until this directive had been called, none of the in-core edits which had been made in the session 
would be saved. When the directive was made, the persister would go through all functions/symbols 
which had been edited during the session, and if they had package metadata would immediately save 
them; if they had no package metadata would prompt for it. 

## Working notes

### 20130919 13:20

The function 'source-fn' in package 'clojure.repl' returns, as a string, the source of the 
function (or macro) whose name is passed to it as argument. It does this by checking the metadata 
associated with the function object using the 'meta' function. This metadata (if present) is a map 
containing the keys ':file' and ':line'. I'm guessing, therefore, that this metadata is set up while
reading the source file.

The function 'read-string' can be used to parse a string into an S-expression. I'm taking it 
as read that the string returned by source-fn will always be a single well-formed S-expression.

As a first pass, I'll write a function sedit which takes an s-expression as argument, pretty prints 
it to the screen, and awaits a key stroke from the user. The following keys will be recognised:

* A: ['CAR'] call sedit recursively on the CAR of the current s-expression; 
		return a cons of the result of this with the cdr of the current s-expression. Obviously, only 
		available if the current s-expression is a list with at least one element.
* D: ['CDR'] call sedit recursively on the CDR of the current s-expression;
	return a cons of the CAR of the current s-expression with the result of this. Obviously, only
	available if the current s-expression is a list.
* S: ['Substitute'] read a new s-expression from the user and return it in place of the 
	current s-exression
* X: ['Cut'] return nil.

### 20130920 10:37

Now sort-of working. One change can be made to an s-expression, and it can be made anywhere in the 
s-expression. For some reason having made one change you can't then navigate further into the 
s-expression to make another change; I suspect this is a lazy-evaluation problem, but I haven't yet
fixed it.

Also, the 'clear screen' functionality is *extremely* crude, and you have to type a carriage return 
after every command character, which slows down the user interaction badly. For a proof-of-concept
demonstrator that isn't critical, but if anyone is actually going to use this thing it needs to be
fixed.

I've written a wrapper round sedit called fedit, which grabs the source of a function from its 
metadata and passes it to sedit, attempting to redefine the function from the result; this fails for 
the basic clojure 'all data is immutable' reason. But, when you invoke (use 'namespace :reload), it
is able to redefine all the functions, so I must be able to work out how this is done.

## License

Copyright © 2013 Simon Brooke <stillyet-github@googlemail.com>

Distributed under the Eclipse Public License, the same as Clojure.

