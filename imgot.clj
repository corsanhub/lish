#!/usr/bin/env flk
;(ns imgot))

;----------------------------------------------------------------------------------------------------------------------
(defn get-env [name] (get (env) name))

(defn get-pid [] (str (sh* "echo $$") "-" (sh* "echo $BASHPID")))

(defn current-date-str [] (let [date-str (sh* "echo $(date '+%Y-%m-%d %H:%M:%S.%N' |cut -b1-23)")] date-str))

(defn log [value & args]
  (let [line     (str (current-date-str) " - INFO - [" (get-pid) "] - " (if (empty? args) value (str value " " args)))]
    (println "log:" line)
    (sh* (str "echo \"" line "\" >> imgot.log"))))

(defn rand-int [n]
  (sh* (str "echo $(( $RANDOM % " n " ))")))

(defn rand-id [] 
  (let [cmd-str  "echo \"$(date +%s)-$RANDOM\" | sha256sum | base64 | head -c 32 ; echo "]
    (log "create-rnd-id command: " cmd-str)
    (sh* cmd-str)))

(defn rm-file [file]
  (log (str "Removing file: [" file "] ..."))
  (sh* (str "rm -f " file)))

(defn mv-file [source target]
    (log (str "Moving file: [" source "] -> [" target "] ..."))
    (sh* (str "mv " source " " target)))

(defn f* [a b] (sh* (str "echo \"scale=6 ; 1.0*" a "*" b "\" | bc")))

(defn f/ [a b] (sh* (str "echo \"scale=6 ; 1.0*" a "/" b "\" | bc")))

(defn f+ [a b] (sh* (str "echo \"scale=6 ; 0.0+" a "+" b "\" | bc")))

(defn f- [a b] (sh* (str "echo \"scale=6 ; 0.0+" a "-" b "\" | bc")))

(defn deg-to-rad [angle] (f/ angle 57.2957795131))

(defn cos [radians] (sh* (str "echo $(bc -l <<< \"c(" radians ")\")")))

(defn sin [radians] (sh* (str "echo $(bc -l <<< \"s(" radians ")\")")))

(defn floor [float-value] (sh* (str "X=$(echo \"" float-value "\" | bc) ; echo ${X%.*}")))
;----------------------------------------------------------------------------------------------------------------------

(defn get-dimension [fileName]
  (let [width  (sh* (str "identify -format \"%w\" \"" fileName "\""))
        height (sh* (str "identify -format \"%h\" \"" fileName "\""))]
      {:width width :height height}))

(defn create-empty-img [width height ratio]
  (let [tmp-name  (str "tmp-" (rand-id) ".png")
        _         (log (str "Creating empty image: [" tmp-name "] ..."))
        empty-cmd (str "convert -size $((" width "*" ratio "))x$((" height "*" ratio ")) xc:transparent \"" tmp-name "\"")]
    (log "empty-cmd:" empty-cmd)
    (sh* empty-cmd)
    tmp-name))

(defn scale-image [input-image percentage]
  (let [tmp-name  (str "tmp-" (rand-id) ".png")
        _         (log (str "Creating empty image: [" tmp-name "] ..."))
        scale-cmd (str "convert " input-image " -resize " percentage "% \"" tmp-name "\"")]
    (log "scale-cmd:" scale-cmd)
    (sh* scale-cmd)
    tmp-name))

(defn trim-image [input-image bg-color]
      (let [tmp-name (str "tmp-" (rand-id) ".png")
            _        (log (str "Trimming the image: [" tmp-name "] ..."))
            trim-cmd (str "convert \"" input-image "\" -trim -fuzz 2% +repage \"" tmp-name "\"")
            ]
        (log "trim-cmd:" trim-cmd)
        (sh* trim-cmd)
        tmp-name))

(defn merge-images [base-image image-a position]
  (log (str "Merging images ..."))
  (let [merged-name (str "tmp-" (rand-id) ".png")
        merge-cmd  (str "convert \"" base-image "\" \"" image-a "\" "
                        "  -gravity " position " "
                        "  -compose over -composite \"" merged-name "\"")]
    (log "merge-cmd:" merge-cmd)
    (sh* merge-cmd)
    merged-name))

(defn apply-perspective [input-image ratio angle]
  (let [tmp-name     (str "tmp-" (rand-id) ".png")
        dimension    (get-dimension input-image)
        trans-width  (get dimension :width)
        trans-height (get dimension :height)
        width   (f/ trans-width ratio)
        height  (f/ trans-height ratio)
        x0      (f/ (f- trans-width width) 2.0)
        y0      (f/ (f- trans-height height) 2.0)
        x1      (f+ x0 width)
        y1      (f+ y0 height)
        _       (log "p0:" x0 "," y0)
        _       (log "p1:" x1 "," y1)
        radians (deg-to-rad angle)
        cosx    (cos radians)
        sinx    (sin radians)
        hyp     width
        ca      (f* cosx hyp)
        co      (f* sinx hyp)
        y0-     (f- y0 co)
        y1-     (f- y1 co)
        x0+     (f+ x0 (f- width ca))
        _       (log "radians:" radians)
        _       (log "cosx:" cosx)
        _       (log "sinx:" sinx)
        _       (log "ca:" ca)
        _       (log "co:" co)
        _       (log "y0-:" y0-)
        _       (log "y1-:" y1-)
        _       (log "x0+:" x0+)
        s00     (str x0 "," y0 " " x0+ "," y0-)
        s10     (str x1 "," y0 " " x1 "," y0)
        s01     (str x0 "," y1 " " x0+ "," y1-)
        s11     (str x1 "," y1 " " x1 "," y1)
        transform-str   (str s00 "  " s10 "  " s01 "  " s11 )
        perspective-cmd (str "convert \"" input-image "\" -alpha set -virtual-pixel transparent -distort Perspective \"" transform-str "\" \"" tmp-name "\"")
        _               (log "perspective-cmd:" perspective-cmd)]
    (sh* perspective-cmd)
    tmp-name))

(defn transform [input-file output-file]
  (let [angle        30
        scale%       200
        merge-pos    "center"
        scaled-img   (scale-image input-file scale%)
        dimension    (get-dimension scaled-img)
        width        (get dimension :width)
        height       (get dimension :height)
        ratio        (floor (f/ (f* width 2.0) height))
        _            (log "Converting at:" "ratio:" ratio "scale:" scale% " ...")
        base-img     (create-empty-img width height ratio) ;"images/grid400.png" ;
        merged-img   (merge-images base-img scaled-img merge-pos)
        perspect-img (apply-perspective merged-img ratio angle)
        trimmed-img  (trim-image perspect-img "00FF00")]
    (mv-file trimmed-img output-file)
    (rm-file scaled-img)
    (rm-file base-img)
    (rm-file merged-img)
    (rm-file perspect-img)))

(defn main []
  (println "Starting ...")
  ;(println "Environment:" (pr-str (env)))
  (if (= (count *ARGV*) 0)
    (do
      (log "No arguments supplied."))
    (let [inputFile  (nth *ARGV* 0)
          outputFile (nth *ARGV* 1)]
      (log "inputFile:" inputFile)
      (log "outputFile:" outputFile)
      (transform inputFile outputFile)))
  (println "Exiting ..."))

;Execute script
(main)
