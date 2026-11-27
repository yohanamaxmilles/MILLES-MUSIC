# MILLES MUSIC — Design System

Source: Claude redesign supplied by the project owner on 2026-11-27.

## Brand palette
- Background: #050505
- Primary accent: #C6F24E
- Secondary accent: #8B6CF0

## Non-negotiable UI rules
1. Keep the brand palette consistent across screens. Dark and Light themes share the same Lime and Violet accents.
2. On Now Playing, never place the main music cover inside a card, box, or circle. Use full-screen artwork with a subtle 6dp blur and dark gradient scrims.
3. Bottom navigation has no Material pill indicator. The selected item uses a Lime icon and Lime label.
4. Now Playing includes a bottom sheet with Up Next, Lyrics, and Related tabs.
5. The mini player floats above the bottom bar and uses a thin Lime progress line.
6. Use 22–32dp corners for content cards and large typography with tight letter spacing.

## Implementation notes
- Compose BOM is 2024.09.00, which supports BottomSheetScaffold and HorizontalPager.
- Coil Compose is enabled for artwork rendering.
- Keep the existing project models and ViewModel state. Do not introduce parallel Track models.
- Lyrics UI supports timed lines and auto-scroll. Real lyric data integration is a later phase.
