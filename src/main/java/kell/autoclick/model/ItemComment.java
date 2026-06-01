package kell.autoclick.model;

import kell.autoclick.contants.GlobalConst;

/**
 * A comment row — displayed in the table but skipped during execution.
 * The name field stores the full comment text (including the leading '#').
 */
public class ItemComment extends Item {

    public ItemComment(String text) {
        super(text, 0, 0, GlobalConst.EVENT_COMMENT);
    }

    /** Returns the comment text without the leading '#'. */
    public String getCommentText() {
        String n = getName();
        if (n != null && n.startsWith(GlobalConst.COMMENT_PREFIX)) {
            return n.substring(1).trim();
        }
        return n;
    }
}
