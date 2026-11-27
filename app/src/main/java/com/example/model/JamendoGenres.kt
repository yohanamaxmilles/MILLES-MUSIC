package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Real Jamendo API genre tag descriptors used to browse live /v3.0/tracks/?tags=<tag> queries.
 * Contains no hardcoded sample songs, fake artists, fake albums, or placeholder playlists.
 */
object JamendoGenres {
    val genres: List<GenreCategory> = listOf(
        GenreCategory(
            id = "gen_electronic",
            name = "Electronic",
            subtitle = "Jamendo tag: electronic",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_chill,
                primaryColor = Color(0xFF182A44),
                secondaryColor = Color(0xFF0E131D),
                accentColor = Color(0xFF64B5F6)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_hiphop",
            name = "Hip Hop",
            subtitle = "Jamendo tag: hiphop",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_midnight,
                primaryColor = Color(0xFF242B1E),
                secondaryColor = Color(0xFF151611),
                accentColor = Color(0xFFC6F24E)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_rock",
            name = "Rock",
            subtitle = "Jamendo tag: rock",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_midnight,
                primaryColor = Color(0xFF3E4428),
                secondaryColor = Color(0xFF191C12),
                accentColor = Color(0xFFD8E479)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_pop",
            name = "Pop",
            subtitle = "Jamendo tag: pop",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_hero_music,
                primaryColor = Color(0xFF313525),
                secondaryColor = Color(0xFF151611),
                accentColor = Color(0xFFC6F24E)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_rnb",
            name = "R&B",
            subtitle = "Jamendo tag: rnb",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_afro,
                primaryColor = Color(0xFF472F5C),
                secondaryColor = Color(0xFF1C1426),
                accentColor = Color(0xFF8B6CF0)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_jazz",
            name = "Jazz",
            subtitle = "Jamendo tag: jazz",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_afro,
                primaryColor = Color(0xFF6B3E26),
                secondaryColor = Color(0xFF211612),
                accentColor = Color(0xFFE7A95B)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_ambient",
            name = "Ambient",
            subtitle = "Jamendo tag: ambient",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_chill,
                primaryColor = Color(0xFF273454),
                secondaryColor = Color(0xFF141824),
                accentColor = Color(0xFF8B6CF0)
            ),
            trackIds = emptyList()
        ),
        GenreCategory(
            id = "gen_lofi",
            name = "Lo-Fi",
            subtitle = "Jamendo tag: lofi",
            artwork = ArtworkDescriptor(
                imageRes = R.drawable.img_cover_chill,
                primaryColor = Color(0xFF293836),
                secondaryColor = Color(0xFF141B1A),
                accentColor = Color(0xFF9ED4C8)
            ),
            trackIds = emptyList()
        )
    )

    fun jamendoTagForGenre(genreId: String, fallbackName: String): String = when (genreId) {
        "gen_hiphop" -> "hiphop"
        "gen_rnb" -> "rnb"
        "gen_lofi" -> "lofi"
        else -> fallbackName.trim().lowercase().replace("&", "").replace(" ", "").replace("-", "")
    }
}
