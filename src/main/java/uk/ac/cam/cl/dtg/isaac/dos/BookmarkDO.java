package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;

/**
 * DO representing a bookmarked piece of content.
 */
public record BookmarkDO(String contentId, Date created) {}
