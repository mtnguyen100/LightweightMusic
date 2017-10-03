package com.theroungelounge.musicappone;

import android.content.Context;
import android.widget.MediaController;

/**
 * Created by Rounge on 6/2/2016.
 * TODO: Explore the MediaController class to see what methods you can override to make
 * TODO: the MusicController better.
 */
public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    /**
     * Overrides the MediaController hide() method, which
     * hides the MediaController widget after 3 seconds of
     * going idle.
     */
    @Override
    public void hide(){
        super.show();
    }

    public void hide(boolean placeholder) {
        super.hide();
    }
}