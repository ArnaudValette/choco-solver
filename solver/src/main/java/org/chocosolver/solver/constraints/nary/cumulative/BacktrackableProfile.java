package org.chocosolver.solver.constraints.nary.cumulative;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import java.util.List;

public class BacktrackableProfile {
    private final IStateInt[] timePoints;
    private final IStateInt[] heights;
    private final EventPointSeries eventPointSeries;
    private final IStateInt idx;

    public BacktrackableProfile(int nbTasks, Model model) {
        idx = model.getEnvironment().makeInt(0);
        timePoints = new IStateInt[2 * (nbTasks + 1)];
        heights = new IStateInt[2 * (nbTasks + 1)];
        for (int i = 0; i < timePoints.length; ++i) {
            timePoints[i] = model.getEnvironment().makeInt(0);
            heights[i] = model.getEnvironment().makeInt(0);
        }
        eventPointSeries = new EventPointSeries(nbTasks, 2);
    }

    public void clear() {
        idx.set(0);
    }

    public int size() {
        return idx.get() - 2;
    }

    public int getStartRectangle(int j) {
        return timePoints[j].get();
    }

    public int getEndRectangle(int j) {
        return timePoints[j + 1].get();
    }

    public int getHeightRectangle(int j) {
        return heights[j].get();
    }

    public int buildProfile(List<Task> tasks, List<IntVar> tasksHeights) {
        clear();
        timePoints[idx.get()].set(Integer.MIN_VALUE);
        heights[idx.get()].set(0);
        idx.set(idx.get() + 1);
        int maxHeight = 0;
        eventPointSeries.generateEvents(tasks, false, false, false);
        if (!eventPointSeries.isEmpty()) {
            int h = 0;
            while (!eventPointSeries.isEmpty()) {
                timePoints[idx.get()].set(eventPointSeries.getEvent().getDate());
                while (!eventPointSeries.isEmpty() && eventPointSeries.getEvent().getDate() == timePoints[idx.get()].get()) {
                    Event event = eventPointSeries.removeEvent();
                    h += (event.getType() == Event.SCP ? 1 : -1) * tasksHeights.get(event.getIndexTask()).getLB();
                }
                heights[idx.get()].set(h);
                idx.set(idx.get() + 1);
                maxHeight = Math.max(maxHeight, h);
                assert h >= 0;
            }
            assert h == 0;
        }
        timePoints[idx.get()].set(Integer.MAX_VALUE);
        heights[idx.get()].set(0);
        idx.set(idx.get() + 1);
        return maxHeight;
    }

    public int find(int date) {
        int i1 = 0;
        int i2 = idx.get() - 2;
        while (i1 < i2) {
            int im = (i1 + i2) / 2;
            if (timePoints[im].get() <= date && date < timePoints[im + 1].get()) {
                i1 = im;
                i2 = im;
            } else if (timePoints[im].get() < date) {
                i1 = im + 1;
            } else if (timePoints[im].get() > date) {
                i2 = im - 1;
            }
        }
        return i1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Profile[");
        if (size() > 0) {
            for (int i = 0; i < size(); i++) {
                sb.append("<").append(timePoints[i]).append(",").append(timePoints[i + 1]).append(",").append(heights[i]).append(">");
                if (i < size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("<").append(timePoints[size()]).append(",").append(Integer.MAX_VALUE).append(",").append(0).append(">");
        } else {
            sb.append("<").append(Integer.MIN_VALUE).append(",").append(Integer.MAX_VALUE).append(",").append(0).append(">");
        }
        sb.append("]");
        return sb.toString();
    }
}
