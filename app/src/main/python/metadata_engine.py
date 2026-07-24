import json
from ytmusicapi import YTMusic

_ytmusic = YTMusic()

def get_up_next(video_id: str, limit: int = 10) -> str:
    result = _ytmusic.get_watch_playlist(videoId=video_id, limit=limit + 5)
    tracks = result.get("tracks", [])

    up_next = []
    for track in tracks:
        track_video_id = track.get("videoId")
        if not track_video_id or track_video_id == video_id:
            continue

        artists = track.get("artists") or []
        artist_name = artists[0]["name"] if artists and artists[0].get("name") else ""

        thumbnails = track.get("thumbnail") or []
        thumbnail_url = thumbnails[-1]["url"] if thumbnails else None

        up_next.append({
            "videoId": track_video_id,
            "title": track.get("title", "Unknown title"),
            "artist": artist_name,
            "thumbnailUrl": thumbnail_url,
        })

        if len(up_next) >= limit:
            break

    return json.dumps(up_next)
