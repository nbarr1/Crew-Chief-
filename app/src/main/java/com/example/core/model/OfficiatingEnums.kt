package com.example.core.model

enum class OfficiatingTier(
    val displayName: String,
    val description: String,
    val crewSize: Int,
    val minRatingRequired: Double,
    val spottingToleranceYards: Double
) {
    YOUTH(
        displayName = "Youth League",
        description = "Instructional youth level. Focus on basic mechanics, player safety, and fundamental boundary calls.",
        crewSize = 3,
        minRatingRequired = 0.0,
        spottingToleranceYards = 1.5
    ),
    PREP(
        displayName = "High School Prep",
        description = "5-official mechanics. Expanded zone coverage, false starts, holding, and sideline chain management.",
        crewSize = 5,
        minRatingRequired = 65.0,
        spottingToleranceYards = 1.0
    ),
    SMALL_COLLEGE(
        displayName = "Small College (D3/NAIA)",
        description = "7-official mechanics. Fast spread offenses, pass interference judgment, and tighter spotting precision.",
        crewSize = 7,
        minRatingRequired = 75.0,
        spottingToleranceYards = 0.75
    ),
    MAJOR_COLLEGE(
        displayName = "Major College (FBS)",
        description = "8-official mechanics (Center Judge added). Up-tempo hurry-up snaps, targeting reviews, and high sideline pressure.",
        crewSize = 8,
        minRatingRequired = 85.0,
        spottingToleranceYards = 0.5
    ),
    SEMI_PRO(
        displayName = "Semi-Pro / Spring League",
        description = "Hard-hitting veterans, aggressive line battles, and tight dead-ball altercation management.",
        crewSize = 8,
        minRatingRequired = 90.0,
        spottingToleranceYards = 0.35
    ),
    PROFESSIONAL(
        displayName = "Professional Championship Tier",
        description = "Elite national television spotlight. Millisecond reaction windows, sub-yard spotting accuracy, and high stakes.",
        crewSize = 8,
        minRatingRequired = 95.0,
        spottingToleranceYards = 0.25
    );

    val nextTier: OfficiatingTier?
        get() = when (this) {
            YOUTH -> PREP
            PREP -> SMALL_COLLEGE
            SMALL_COLLEGE -> MAJOR_COLLEGE
            MAJOR_COLLEGE -> SEMI_PRO
            SEMI_PRO -> PROFESSIONAL
            PROFESSIONAL -> null
        }
}

enum class OfficialPosition(
    val abbrev: String,
    val fullName: String,
    val onFieldLocation: String,
    val primaryResponsibility: String
) {
    REFEREE(
        abbrev = "R",
        fullName = "Referee (Crew Chief)",
        onFieldLocation = "Offensive Backfield (Passing Arm Side)",
        primaryResponsibility = "Passer protection, roughing/grounding, forward pass vs fumble, signal announcements, crew conferences."
    ),
    UMPIRE(
        abbrev = "U",
        fullName = "Umpire",
        onFieldLocation = "Offensive Backfield / Defensive Line",
        primaryResponsibility = "Interior line play, offensive holding, illegal blocks, false starts, ball spotting between downs."
    ),
    DOWN_JUDGE(
        abbrev = "DJ",
        fullName = "Down Judge",
        onFieldLocation = "Near Sideline (Line of Scrimmage)",
        primaryResponsibility = "Neutral zone, offside, encroachment, forward progress, out of bounds, chain crew supervision."
    ),
    LINE_JUDGE(
        abbrev = "LJ",
        fullName = "Line Judge",
        onFieldLocation = "Far Sideline (Line of Scrimmage)",
        primaryResponsibility = "Far line of scrimmage, illegal motion, passer crossing line, near side forward progress."
    ),
    FIELD_JUDGE(
        abbrev = "FJ",
        fullName = "Field Judge",
        onFieldLocation = "Deep Sideline (20-25 yds downfield)",
        primaryResponsibility = "Deep receiver coverage, defensive pass interference, catch/no-catch boundary, goal line pylon."
    ),
    SIDE_JUDGE(
        abbrev = "SJ",
        fullName = "Side Judge",
        onFieldLocation = "Opposite Deep Sideline (20-25 yds downfield)",
        primaryResponsibility = "Deep receiver coverage, secondary collisions, punt/kick sideline coverage, game clock backup."
    ),
    BACK_JUDGE(
        abbrev = "BJ",
        fullName = "Back Judge",
        onFieldLocation = "Deep Middle (25 yds downfield)",
        primaryResponsibility = "Defensive player count (11 men), tight end release, deep middle passes, scoring kick uprights, play clock."
    ),
    CENTER_JUDGE(
        abbrev = "CJ",
        fullName = "Center Judge",
        onFieldLocation = "Offensive Backfield (Opposite Referee)",
        primaryResponsibility = "Rapid ball spotting for up-tempo offenses, interior offensive tackle/guard blocks, backfield mechanics."
    ),
    REPLAY_OFFICIAL(
        abbrev = "RO",
        fullName = "Replay Official",
        onFieldLocation = "Press Box Replay Booth",
        primaryResponsibility = "Indisputable video review, catch/fumble turnover verification, targeting confirmations under review timer."
    )
}

enum class CallGrade(
    val label: String,
    val scoreDelta: Double,
    val isFoulRelated: Boolean
) {
    CORRECT_CALL(
        label = "Correct Call",
        scoreDelta = +5.0,
        isFoulRelated = true
    ),
    CORRECT_NON_CALL(
        label = "Correct Non-Call",
        scoreDelta = +3.0,
        isFoulRelated = false
    ),
    INCORRECT_CALL(
        label = "Incorrect Call (Ghost Flag)",
        scoreDelta = -7.0,
        isFoulRelated = true
    ),
    MISSED_CALL(
        label = "Missed Infraction",
        scoreDelta = -6.0,
        isFoulRelated = true
    ),
    MARGINAL_CALL(
        label = "Marginal Decision",
        scoreDelta = +1.0,
        isFoulRelated = true
    ),
    UNNECESSARY_CALL(
        label = "Over-Officiated / Unnecessary",
        scoreDelta = -4.0,
        isFoulRelated = true
    )
}
