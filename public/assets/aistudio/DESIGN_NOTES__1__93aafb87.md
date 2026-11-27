# MILLES MUSIC - Design System (source of truth for Google AI Studio)

Dependencies: coil-compose 2.6.0, androidx.compose.material:material-icons-extended, Compose BOM >= 2024.02.
MainActivity must call enableEdgeToEdge().

## Navigation (FIXED - do not change)
- EXACTLY 4 destinations: Home, Explore, Library, Search. Nothing else in the bottom bar.
- Component: FloatingNav (floating pill with 3 tabs + separate round Search button). Do NOT use Material NavigationBar.
- Favorites is NOT a tab: it is the "Liked Songs" segment inside Library.
- Profile is NOT a tab: it opens from the avatar at top-right of Home.
- Mini player floats above FloatingNav (see MillesShell). Content scrolls under both.

## Now Playing (FIXED layout)
Header (close + "PLAYING FROM") -> full-screen blurred cover (6dp) -> title/artist + like + more
-> scrubber with elapsed / -remaining -> prev / PLAY / next -> utilities row (lyrics | quality | queue).
Shuffle & repeat live inside the queue sheet. Sheet tabs: Up Next, Lyrics, Related.
NEVER put the cover in a box or circle. NEVER use Material default sliders here.

## Style
Colors: #050505 background, #C6F24E lime accent, #8B6CF0 violet. Large bold titles (34sp), 20dp screen margins, 20-32dp corner radius, glass buttons (GlassIconButton).
Use outlined icons when unselected, filled when selected.
