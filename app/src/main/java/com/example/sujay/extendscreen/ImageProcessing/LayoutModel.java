package com.example.sujay.extendscreen.ImageProcessing;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sujay on 29/8/15.
 */
public class LayoutModel
{
    private List<Integer[]> layout;
    static LayoutModel LAYOUT = null;
    private LayoutModel()
    {
        layout = new ArrayList<Integer[]>();
    }
    public static LayoutModel getSingleton()
    {
        if(LAYOUT==null)
            LAYOUT = new LayoutModel();
        return LAYOUT;
    }
    public void setLayout(List<Integer[]> layout)
    {
        this.layout.clear();
        this.layout.addAll(layout);
    }
    public List<Integer[]> getLayout()
    {
        if(layout.size()==0)
            return null;
        return layout;
    }
}
