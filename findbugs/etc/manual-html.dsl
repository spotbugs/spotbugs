<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY dbstyle SYSTEM "/export/home/daveho/linux/docbook/docbook-dsssl-1.78/html/docbook.dsl" CDATA DSSSL>
]>

<style-sheet>
<style-specification use="docbook">
<style-specification-body>

;; Enumerate chapters and sections
(define %chapter-autolabel% #t)
(define %section-autolabel% #t)

;; Root of document is "index"
(define %root-filename% "index")

;; Base HTML filenames on id attribute values
(define %use-id-as-filename% #t)

;; Use extension ".html"
(define %html-ext% ".html")

;; When we actually generate the manual into the "doc" directory
;; of the FindBugs working directory, we'll place it in the
;; "manual" subdirectory.
(define use-output-dir #t)
(define %output-dir% "manual")

</style-specification-body>
</style-specification>
<external-specification id="docbook" document="dbstyle">
</style-sheet>
