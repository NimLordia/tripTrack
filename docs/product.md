# Product scope

TripTrack records motorcycle rides for later analysis during a long stop or at the end of a trip. Initial use is the owner's Android phone on weekend twisty-road trips. Recording should require minimal interaction while riding.

V1 requires ride time, distance, average speed, **credible recorded top speed**, stop detection and editable stop exclusions. Top speed cannot be replaced by the highest minute-average. Inspect useful telemetry across selected route sections and time intervals.

V1 is portrait only. External hardware belongs to [later review](later/index.md). iOS, social features, automatic ride start, corner scores and embedded navigation are not approved features.

The user approved organizing repository files and scaffolding Kotlin, Compose, Room and Firebase. Scaffold components do not establish that the complete V1 recorder works.
