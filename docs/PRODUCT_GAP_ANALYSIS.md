# CINEMAX product gap analysis

Reference products reviewed: Nuvio Mobile (official product page and current mobile release notes), Stremio, and advanced mobile video-player capabilities. Nuvio current mobile release notes include custom subtitle styling, chapter markers, player engine failover, and improved PiP behavior. This is a capability comparison, not a claim that CINEMAX already implements every feature.

## Delivered in this UI pass
- Arabic-first dark visual system with selectable violet, blue, rose, emerald, and amber accent colors.
- OLED-black option and compact media-card density.
- Poster artwork and rating presentation in catalog cards.
- Persistent settings for appearance, resume preference, autoplay preference, and preferred audio/subtitle languages.
- Shared brand mark, Android launcher vector, and generated iOS AppIcon pipeline.
- Settings sections for appearance, playback, languages, and addon management.

## Gap matrix

| Capability | Nuvio | Stremio | CINEMAX status |
| --- | --- | --- | --- |
| Mobile-first catalog browsing | Yes | Yes | Basic catalog cards; more screen polish needed |
| Artwork, ratings, metadata | Yes | Yes | Posters, available metadata and ratings from addon responses; rating sources not aggregated |\n| Custom subtitle appearance and font/render modes | Yes | Addon/player dependent | Not implemented |\n| Player engine fallback and playback diagnostics | Yes | Player dependent | Native AVPlayer / Media3 only; no fallback policy |\n| Chapters and chapter markers | Yes | Addon dependent | Not implemented |\n| Picture-in-picture | Platform support | Platform support | Not yet verified end-to-end |
| Addon catalogs | Yes | Core model | Stremio manifest/catalog support; no Nuvio JS runtime |
| Cross-device sync | Account sync | Account sync | Not implemented |
| Multiple profiles | Yes | Limited/account based | Not implemented |
| Continue watching/history | Yes | Yes | Preference saved; watch history/progress backend not implemented |
| Subtitle discovery and selection | Yes | Addon based | Not implemented in player |
| Audio/subtitle track controls | Yes | Yes | Not exposed yet |
| Intro/recap/outro skipping | Yes | Addon dependent | Not implemented |
| External player selection | Yes | Supported workflows | Not implemented |
| Playback speed/PiP/gestures | Platform-specific | Platform-specific | Not implemented in shared controls |
| Themes/OLED/accent colors | Yes | Limited | Implemented in settings |
| Privacy/security boundary | Client-side | Remote addons | HTTPS validation; executable JS remains disabled |

## Priority roadmap

### P0 — iPhone experience
1. Finish the home screen hierarchy: featured title, continue watching, category shelves, and clear bottom navigation.
2. Replace metadata dialogs with a full detail screen including artwork, synopsis, seasons/episodes, and source selection.
3. Implement favorites and watch progress locally before adding cloud sync.
4. Implement a real player settings overlay: track selection, subtitle styling, speed, aspect ratio, and picture-in-picture where supported.
5. Test VoiceOver, Dynamic Type, safe areas, and iPhone portrait/landscape behavior.

### P1 — media features
1. Subtitle provider integration and external subtitle loading.
2. Cross-addon search and source sorting/quality badges.
3. Continue-watching and episode progression persistence.
4. Optional Trakt/Simkl integrations, only after explicit account linking and secure token storage.
5. Intro/recap/outro markers through a supported metadata service.

### P2 — ecosystem
1. Profile support and cloud sync.
2. Nuvio plugin repository discovery only after a hardened JavaScript sandbox exists.
3. Chromecast/AirPlay and external player handoff.
4. TV-specific navigation and remote focus behavior.

## Important product constraints
- CINEMAX is a media client; it does not host or bundle media.
- Addon code must never be evaluated as ordinary JavaScript. The Nuvio runtime remains blocked until a sandbox with resource limits and explicit trust controls is available.
- A preference toggle is not evidence of a feature. Playback preferences must be wired into player behavior before being advertised as active.
- iOS AppIcon generation happens in macOS CI from the shared SVG asset.
