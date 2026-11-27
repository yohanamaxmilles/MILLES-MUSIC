package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.LyricLine

object SampleCatalog {

    private val artAfro = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_afro,
        primaryColor = Color(0xFF6B3E26),
        secondaryColor = Color(0xFF211612),
        accentColor = Color(0xFFE7A95B)
    )

    private val artMidnight = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_midnight,
        primaryColor = Color(0xFF242B1E),
        secondaryColor = Color(0xFF151611),
        accentColor = Color(0xFFC6F24E)
    )

    private val artChill = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_chill,
        primaryColor = Color(0xFF273454),
        secondaryColor = Color(0xFF141824),
        accentColor = Color(0xFF8B6CF0)
    )

    private val artHero = ArtworkDescriptor(
        imageRes = R.drawable.img_hero_music,
        primaryColor = Color(0xFF313525),
        secondaryColor = Color(0xFF151611),
        accentColor = Color(0xFFC6F24E)
    )

    private val artElectronic = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_chill,
        primaryColor = Color(0xFF182A44),
        secondaryColor = Color(0xFF0E131D),
        accentColor = Color(0xFF64B5F6),
        patternVariant = 1
    )

    private val artRock = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_midnight,
        primaryColor = Color(0xFF3E4428),
        secondaryColor = Color(0xFF191C12),
        accentColor = Color(0xFFD8E479),
        patternVariant = 2
    )

    private val artRnB = ArtworkDescriptor(
        imageRes = R.drawable.img_hero_music,
        primaryColor = Color(0xFF472F5C),
        secondaryColor = Color(0xFF1C1426),
        accentColor = Color(0xFF8B6CF0),
        patternVariant = 3
    )

    private val artLoFi = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_chill,
        primaryColor = Color(0xFF293836),
        secondaryColor = Color(0xFF141B1A),
        accentColor = Color(0xFF9ED4C8),
        patternVariant = 4
    )

    private val artWorkout = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_afro,
        primaryColor = Color(0xFF2E3C2B),
        secondaryColor = Color(0xFF131912),
        accentColor = Color(0xFFA8D83A),
        patternVariant = 5
    )

    private val artSoul = ArtworkDescriptor(
        imageRes = R.drawable.img_cover_afro,
        primaryColor = Color(0xFF5A372B),
        secondaryColor = Color(0xFF1F1411),
        accentColor = Color(0xFFE5987E),
        patternVariant = 6
    )

    val tracks: List<Track> = listOf(
        Track(
            id = "trk_nakujoa",
            title = "Nakujoa",
            artist = "Diamond Platnumz",
            album = "Midnight Pulse",
            genre = "Afrobeats",
            durationSeconds = 232,
            artwork = artMidnight,
            isTrending = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_diamond",
            title = "Diamond",
            artist = "Diamond Platnumz",
            album = "Golden Frequency",
            genre = "Afrobeats",
            durationSeconds = 208,
            artwork = artAfro,
            isTrending = true,
            isNewRelease = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_velvet_horizon",
            title = "Velvet Horizon",
            artist = "Snoh Aalegra",
            album = "Quiet Hours",
            genre = "R&B",
            durationSeconds = 244,
            artwork = artChill,
            isTrending = true
        ),
        Track(
            id = "trk_neon_rain",
            title = "Neon Rain",
            artist = "Kaytranada",
            album = "Electric Dusk",
            genre = "Electronic",
            durationSeconds = 196,
            artwork = artElectronic,
            isTrending = true,
            isNewRelease = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_diamond_girl",
            title = "Diamond Girl",
            artist = "Boma",
            album = "Lagos After Dark",
            genre = "Afrobeats",
            durationSeconds = 189,
            artwork = artAfro,
            isNewRelease = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_solar_drift",
            title = "Solar Drift",
            artist = "Fred again..",
            album = "Actual Life Echoes",
            genre = "Electronic",
            durationSeconds = 258,
            artwork = artHero,
            isTrending = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_amber_tide",
            title = "Amber Tide",
            artist = "Tems",
            album = "Born in the Wild",
            genre = "R&B",
            durationSeconds = 214,
            artwork = artSoul,
            isNewRelease = true
        ),
        Track(
            id = "trk_analog_sky",
            title = "Analog Sky",
            artist = "Nujabes Collective",
            album = "Late Night Tape",
            genre = "Lo-Fi",
            durationSeconds = 175,
            artwork = artLoFi
        ),
        Track(
            id = "trk_diamonds",
            title = "Diamonds",
            artist = "Rihanna",
            album = "Unapologetic",
            genre = "R&B",
            durationSeconds = 225,
            artwork = artRnB,
            isTrending = true
        ),
        Track(
            id = "trk_chrome_echo",
            title = "Chrome Echo",
            artist = "Kendrick Lamar",
            album = "Signal & Noise",
            genre = "Hip Hop",
            durationSeconds = 241,
            artwork = artWorkout,
            isTrending = true,
            isUpbeat = true
        ),
        Track(
            id = "trk_glass_canyon",
            title = "Glass Canyon",
            artist = "Arctic Monkeys",
            album = "Velvet Static",
            genre = "Rock",
            durationSeconds = 219,
            artwork = artRock,
            isNewRelease = true
        ),
        Track(
            id = "trk_lavender_mist",
            title = "Lavender Mist",
            artist = "Cleo Sol",
            album = "Quiet Hours",
            genre = "Chill Vibes",
            durationSeconds = 263,
            artwork = artChill
        )
    )

    val artists: List<Artist> = listOf(
        Artist(
            id = "art_diamond",
            name = "Diamond Platnumz",
            genre = "Afrobeats",
            monthlyListeners = "6.8M monthly listeners",
            artwork = artMidnight,
            bio = "Bongo Flava and Afrobeats icon blending rhythmic East African percussion with modern club production."
        ),
        Artist(
            id = "art_tems",
            name = "Tems",
            genre = "R&B · Soul",
            monthlyListeners = "18.4M monthly listeners",
            artwork = artHero,
            bio = "Warm, hypnotic alt-R&B vocals layered over minimalist basslines and lush analog chords."
        ),
        Artist(
            id = "art_kaytranada",
            name = "Kaytranada",
            genre = "Electronic",
            monthlyListeners = "11.2M monthly listeners",
            artwork = artElectronic,
            bio = "Genre-bending electronic producer crafting nocturnal house grooves and soulful synth textures."
        ),
        Artist(
            id = "art_fred",
            name = "Fred again..",
            genre = "Electronic",
            monthlyListeners = "15.1M monthly listeners",
            artwork = artChill,
            bio = "Emotional dance floor anthems woven from intimate field recordings and euphoric piano chords."
        ),
        Artist(
            id = "art_boma",
            name = "Boma",
            genre = "Afrobeats",
            monthlyListeners = "2.9M monthly listeners",
            artwork = artAfro,
            bio = "Crisp guitar-driven Afro-fusion storyteller bringing late-night coastal grooves."
        ),
        Artist(
            id = "art_rihanna",
            name = "Rihanna",
            genre = "R&B · Pop",
            monthlyListeners = "82.3M monthly listeners",
            artwork = artRnB,
            bio = "Global powerhouse known for bold anthems, crystalline vocals, and timeless production."
        )
    )

    val albums: List<Album> = listOf(
        Album(
            id = "alb_diamond_dreams",
            title = "Diamond Dreams",
            artist = "Various Artists",
            year = 2026,
            genre = "Afrobeats",
            trackIds = listOf("trk_nakujoa", "trk_diamond", "trk_diamond_girl"),
            artwork = artWorkout
        ),
        Album(
            id = "alb_midnight_pulse",
            title = "Midnight Pulse",
            artist = "Diamond Platnumz",
            year = 2025,
            genre = "Afrobeats",
            trackIds = listOf("trk_nakujoa", "trk_diamond"),
            artwork = artMidnight
        ),
        Album(
            id = "alb_quiet_hours",
            title = "Quiet Hours",
            artist = "Snoh Aalegra & Cleo Sol",
            year = 2026,
            genre = "R&B",
            trackIds = listOf("trk_velvet_horizon", "trk_lavender_mist"),
            artwork = artChill
        ),
        Album(
            id = "alb_electric_dusk",
            title = "Electric Dusk",
            artist = "Kaytranada",
            year = 2025,
            genre = "Electronic",
            trackIds = listOf("trk_neon_rain", "trk_solar_drift"),
            artwork = artElectronic
        )
    )

    val trendingCollections: List<Playlist> = listOf(
        Playlist(
            id = "pl_afrobeats_top",
            title = "Afrobeats",
            subtitle = "Top Hits",
            trackIds = listOf("trk_nakujoa", "trk_diamond", "trk_diamond_girl", "trk_amber_tide"),
            artwork = artAfro,
            displayedSongCount = 180
        ),
        Playlist(
            id = "pl_chill_vibes",
            title = "Chill Vibes",
            subtitle = "Late Night",
            trackIds = listOf("trk_velvet_horizon", "trk_lavender_mist", "trk_analog_sky"),
            artwork = artChill,
            displayedSongCount = 120
        ),
        Playlist(
            id = "pl_electronic_flow",
            title = "Electronic",
            subtitle = "Flow State",
            trackIds = listOf("trk_neon_rain", "trk_solar_drift", "trk_chrome_echo"),
            artwork = artElectronic,
            displayedSongCount = 96
        ),
        Playlist(
            id = "pl_midnight_edit",
            title = "Midnight Edit",
            subtitle = "Curated",
            trackIds = listOf("trk_nakujoa", "trk_diamonds", "trk_glass_canyon"),
            artwork = artMidnight,
            displayedSongCount = 64
        )
    )

    val madeForYouPlaylists: List<Playlist> = listOf(
        Playlist(
            id = "pl_daily_soundscape",
            title = "Lime & Lavender",
            subtitle = "Daily Mix 01",
            trackIds = listOf("trk_solar_drift", "trk_velvet_horizon", "trk_nakujoa", "trk_neon_rain"),
            artwork = artHero,
            displayedSongCount = 45
        ),
        Playlist(
            id = "pl_soul_sessions",
            title = "Velvet Soul",
            subtitle = "Warm R&B",
            trackIds = listOf("trk_amber_tide", "trk_velvet_horizon", "trk_diamonds"),
            artwork = artSoul,
            displayedSongCount = 52
        ),
        Playlist(
            id = "pl_focus_tape",
            title = "Analog Study",
            subtitle = "Lo-Fi & Beats",
            trackIds = listOf("trk_analog_sky", "trk_lavender_mist", "trk_glass_canyon"),
            artwork = artLoFi,
            displayedSongCount = 70
        )
    )

    val initialUserPlaylists: List<Playlist> = listOf(
        Playlist(
            id = "pl_my_favorites",
            title = "My Favorites",
            subtitle = "Curated by you · 245 songs",
            trackIds = listOf("trk_nakujoa", "trk_velvet_horizon", "trk_neon_rain", "trk_amber_tide", "trk_solar_drift"),
            artwork = artAfro,
            displayedSongCount = 245,
            isUserCreated = true
        ),
        Playlist(
            id = "pl_lib_chill",
            title = "Chill Vibes",
            subtitle = "Evening calm · 120 songs",
            trackIds = listOf("trk_velvet_horizon", "trk_lavender_mist", "trk_analog_sky"),
            artwork = artChill,
            displayedSongCount = 120,
            isUserCreated = true
        ),
        Playlist(
            id = "pl_lib_afrobeats",
            title = "Afrobeats Hits",
            subtitle = "Pure energy · 180 songs",
            trackIds = listOf("trk_nakujoa", "trk_diamond", "trk_diamond_girl"),
            artwork = artAfro,
            displayedSongCount = 180,
            isUserCreated = true
        ),
        Playlist(
            id = "pl_lib_workout",
            title = "Workout Mix",
            subtitle = "High tempo · 95 songs",
            trackIds = listOf("trk_chrome_echo", "trk_neon_rain", "trk_solar_drift"),
            artwork = artWorkout,
            displayedSongCount = 95,
            isUserCreated = true
        )
    )

    val genres: List<GenreCategory> = listOf(
        GenreCategory(
            id = "gen_afrobeats",
            name = "Afrobeats",
            subtitle = "Lagos to Dar",
            artwork = artAfro,
            trackIds = listOf("trk_nakujoa", "trk_diamond", "trk_diamond_girl")
        ),
        GenreCategory(
            id = "gen_hiphop",
            name = "Hip Hop",
            subtitle = "Bars & Bass",
            artwork = artMidnight,
            trackIds = listOf("trk_chrome_echo", "trk_diamond_girl")
        ),
        GenreCategory(
            id = "gen_electronic",
            name = "Electronic",
            subtitle = "Synth & Pulse",
            artwork = artElectronic,
            trackIds = listOf("trk_neon_rain", "trk_solar_drift")
        ),
        GenreCategory(
            id = "gen_rock",
            name = "Rock",
            subtitle = "Electric Guitars",
            artwork = artRock,
            trackIds = listOf("trk_glass_canyon")
        ),
        GenreCategory(
            id = "gen_rnb",
            name = "R&B",
            subtitle = "Smooth Vocals",
            artwork = artRnB,
            trackIds = listOf("trk_velvet_horizon", "trk_amber_tide", "trk_diamonds")
        ),
        GenreCategory(
            id = "gen_lofi",
            name = "Lo-Fi",
            subtitle = "Warm Tape",
            artwork = artLoFi,
            trackIds = listOf("trk_analog_sky", "trk_lavender_mist")
        )
    )

    val initialFavoriteTrackIds: Set<String> = setOf(
        "trk_nakujoa",
        "trk_velvet_horizon",
        "trk_neon_rain",
        "trk_amber_tide",
        "trk_solar_drift"
    )

    private val sampleLyricsByTrackId: Map<String, List<LyricLine>> = mapOf(
        "trk_nakujoa" to listOf(
            LyricLine(0L, "City lights hum low across the bay"),
            LyricLine(14_000L, "Bassline heavy till the break of day"),
            LyricLine(28_000L, "Every step feels lighter in the sound"),
            LyricLine(42_000L, "Lime and midnight spinning all around"),
            LyricLine(58_000L, "Turn the rhythm up and let it breathe"),
            LyricLine(76_000L, "Everything we wanted underneath")
        ),
        "trk_velvet_horizon" to listOf(
            LyricLine(0L, "Shadows stretching out on quiet glass"),
            LyricLine(18_000L, "Hold the moment while the colors pass"),
            LyricLine(36_000L, "Warm reverb echoing across the room"),
            LyricLine(54_000L, "Soft lavender rising in the gloom")
        )
    )

    fun lyricsForTrack(trackId: String): List<LyricLine> = sampleLyricsByTrackId[trackId].orEmpty()

    fun trackById(id: String): Track? = tracks.find { it.id == id }
    fun tracksByIds(ids: List<String>): List<Track> = ids.mapNotNull(::trackById)
}
