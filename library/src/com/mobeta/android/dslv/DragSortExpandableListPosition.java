/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobeta.android.dslv;

import java.util.ArrayList;


/**
 * ExpandableListPosition can refer to either a group's position or a child's
 * position. Referring to a child's position requires both a group position (the
 * group containing the child) and a child position (the child's position within
 * that group). To create objects, use {@link #obtainChildPosition(int, int)} or
 * {@link #obtainGroupPosition(int)}.
 */
class DragSortExpandableListPosition {
    
    private static final int MAX_POOL_SIZE = 5;
    private static ArrayList<DragSortExpandableListPosition> sPool =
        new ArrayList<DragSortExpandableListPosition>(MAX_POOL_SIZE);
    
    /**
     * This data type represents a child position
     */
    public final static int CHILD = 1;

    /**
     * This data type represents a group position
     */
    public final static int GROUP = 2;

    /**
     * The position of either the group being referred to, or the parent
     * group of the child being referred to
     */
    public int groupPos;

    /**
     * The position of the child within its parent group 
     */
    public int childPos;

    /**
     * The position of the item in the flat list (optional, used internally when
     * the corresponding flat list position for the group or child is known)
     */
    int flatListPos;
    
    /**
     * What type of position this ExpandableListPosition represents
     */
    public int type;
    
    private void resetState() {
        groupPos = 0;
        childPos = 0;
        flatListPos = 0;
        type = 0;
    }
    
    private DragSortExpandableListPosition() {
    }
    
    long getPackedPosition() {
        if (type == CHILD) return DragSortExpandableListView.getPackedPositionForChild(groupPos, childPos);
        else return DragSortExpandableListView.getPackedPositionForGroup(groupPos);
    }

    static DragSortExpandableListPosition obtainGroupPosition(int groupPosition) {
        return obtain(GROUP, groupPosition, 0, 0);
    }
    
    static DragSortExpandableListPosition obtainChildPosition(int groupPosition, int childPosition) {
        return obtain(CHILD, groupPosition, childPosition, 0);
    }

    static DragSortExpandableListPosition obtainPosition(long packedPosition) {
        if (packedPosition == DragSortExpandableListView.PACKED_POSITION_VALUE_NULL) {
            return null;
        }
        
        DragSortExpandableListPosition elp = getRecycledOrCreate(); 
        elp.groupPos = DragSortExpandableListView.getPackedPositionGroup(packedPosition);
        if (DragSortExpandableListView.getPackedPositionType(packedPosition) ==
                DragSortExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            elp.type = CHILD;
            elp.childPos = DragSortExpandableListView.getPackedPositionChild(packedPosition);
        } else {
            elp.type = GROUP;
        }
        return elp;
    }
    
    static DragSortExpandableListPosition obtain(int type, int groupPos, int childPos, int flatListPos) {
        DragSortExpandableListPosition elp = getRecycledOrCreate(); 
        elp.type = type;
        elp.groupPos = groupPos;
        elp.childPos = childPos;
        elp.flatListPos = flatListPos;
        return elp;
    }
    
    private static DragSortExpandableListPosition getRecycledOrCreate() {
        DragSortExpandableListPosition elp;
        synchronized (sPool) {
            if (sPool.size() > 0) {
                elp = sPool.remove(0);
            } else {
                return new DragSortExpandableListPosition();
            }
        }
        elp.resetState();
        return elp;
    }
    
    /**
     * Do not call this unless you obtained this via ExpandableListPosition.obtain().
     * PositionMetadata will handle recycling its own children.
     */
    public void recycle() {
        synchronized (sPool) {
            if (sPool.size() < MAX_POOL_SIZE) {
                sPool.add(this);
            }
        }
    }
}
