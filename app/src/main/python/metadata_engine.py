import json
from ytmusicapi import YTMusic

_ytmusic = YTMusic()

def _thumbnail_url(thumbnails):
    return thumbnails[-1]["url"] if thumbnails else None

def _artist_name(artists):
    return artists[0]["name"] if artists and artists[0].get("name") else ""

def get_up_next(video_id: str, limit: int = 10) -> str:
    result = _ytmusic.get_watch_playlist(videoId=video_id, limit=limit + 5)
    tracks = result.get("tracks", [])

    up_next = []
    for track in tracks:
        track_video_id = track.get("videoId")
        if not track_video_id or track_video_id == video_id:
            continue
        up_next.append({
            "videoId": track_video_id,
            "title": track.get("title", "Unknown title"),
            "artist": _artist_name(track.get("artists")),
            "thumbnailUrl": _thumbnail_url(track.get("thumbnail") or []),
        })
        if len(up_next) >= limit:
            break

    return json.dumps(up_next)

def search_songs(query: str, limit: int = 20) -> str:
    results = _ytmusic.search(query, filter="songs", limit=limit)

    items = []
    for entry in results:
        video_id = entry.get("videoId")
        if not video_id:
            continue
        items.append({
            "videoId": video_id,
            "title": entry.get("title", "Unknown title"),
            "artist": _artist_name(entry.get("artists")),
            "thumbnailUrl": _thumbnail_url(entry.get("thumbnails") or []),
        })

    return json.dumps(items)

def get_trending(limit: int = 15) -> str:
    items = []
    try:
        charts = _ytmusic.get_charts(country="IN")
        videos_section = charts.get("videos") or charts.get("trending") or {}

        raw_list = videos_section.get("items") if isinstance(videos_section, dict) else videos_section
        raw_list = raw_list or []

        for entry in raw_list:
            video_id = entry.get("videoId")
            if not video_id:
                continue
            items.append({
                "videoId": video_id,
                "title": entry.get("title", "Unknown title"),
                "artist": _artist_name(entry.get("artists")),
                "thumbnailUrl": _thumbnail_url(entry.get("thumbnails") or []),
            })
    except Exception:
        items = []

    if not items:
        fallback = json.loads(search_songs("trending music 2026", limit=limit))
        items = fallback

    return json.dumps(items[:limit])
