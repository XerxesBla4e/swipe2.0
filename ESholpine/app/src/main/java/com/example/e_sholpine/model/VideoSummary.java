package com.example.e_sholpine.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoSummary {
    private String videoId;
    private String thumbnailUri;
    private Long watchCount;

    private List<String> hashtags;

    public VideoSummary(String videoId, String thumbnailUri, Long watchCount, List<String> hashtags) {
        this.videoId = videoId;
        this.thumbnailUri = thumbnailUri;
        this.watchCount = watchCount;
        this.hashtags = hashtags;
    }

    public VideoSummary() {
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public Long getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(Long watchCount) {
        this.watchCount = watchCount;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("thumbnailUri", thumbnailUri);
        result.put("watchCount", watchCount);
        result.put("hashtags",hashtags);

        return result;
    }
}
