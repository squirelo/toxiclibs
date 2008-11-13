package toxi.color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import toxi.math.MathUtils;

public class ColorList {

	protected ArrayList colours = new ArrayList();

	public ColorList() {

	}

	public ColorList(ArrayList colours) {
		this.colours.addAll(colours);
	}

	public ColorList(Color[] colourArray) {
		for (int i = 0; i < colourArray.length; i++)
			colours.add(colourArray[i]);
	}

	public ColorList(int[] argbArray) {
		for (int i = 0; i < argbArray.length; i++) {
			colours.add(Color.newARGB(argbArray[i]));
		}
	}

	public static final ColorList fromARGBImage(int[] pixels, int num,
			boolean uniqueOnly) {
		ArrayList colours = new ArrayList();
		num = MathUtils.min(num, pixels.length);
		int[] index = new int[num];
		for (int i = 0; i < num; i++) {
			int idx;
			if (uniqueOnly) {
				boolean isUnique = true;
				do {
					idx = MathUtils.random(pixels.length);
					for (int j = 0; j < i; j++) {
						if (index[j] == idx) {
							isUnique = false;
							break;
						}
					}
				} while (!isUnique);
			}
			else {
				idx = MathUtils.random(pixels.length);
			}
			index[i] = idx;
			int col = pixels[idx];
			colours.add(Color.newRGBA(((col >> 16) & 0xff) / 255f,
					((col >> 8) & 0xff) / 255f, (col & 0xff) / 255f,
					(col >>> 24) / 255f));
		}
		return new ColorList(colours);
	}

	public ColorList add(Color c) {
		colours.add(c);
		return this;
	}

	public Color getDarkest() {
		Color darkest = null;
		float minBrightness = Float.MAX_VALUE;
		for (Iterator i = colours.iterator(); i.hasNext();) {
			Color c = (Color) i.next();
			if (c.hsv[2] < minBrightness) {
				darkest = c;
				minBrightness = c.hsv[2];
			}
		}
		return darkest;
	}

	public Color getLightest() {
		Color lightest = null;
		float maxBrightness = Float.MIN_VALUE;
		for (Iterator i = colours.iterator(); i.hasNext();) {
			Color c = (Color) i.next();
			if (c.hsv[2] > maxBrightness) {
				lightest = c;
				maxBrightness = c.hsv[2];
			}
		}
		return lightest;
	}

	public Color getAverage() {
		float r = 0;
		float g = 0;
		float b = 0;
		float a = 0;
		for (Iterator i = colours.iterator(); i.hasNext();) {
			Color c = (Color) i.next();
			r += c.rgb[0];
			g += c.rgb[1];
			b += c.rgb[2];
			a += c.alpha;
		}
		int num = colours.size();
		return Color.newRGBA(r / num, g / num, b / num, a / num);
	}

	public ColorList getBlended(float amount) {
		Color[] clrs = (Color[]) colours.toArray(new Color[0]);
		for (int i = 0; i < clrs.length; i++) {
			clrs[i] = clrs[i].getBlended(clrs[i > 0 ? i - 1 : clrs.length - 1],
					amount);
		}
		return new ColorList(clrs);
	}

	public ColorList sortByDistance(boolean isReversed) {
		if (colours.size() == 0)
			return new ColorList();

		// Find the darkest color in the list.
		Color root = getDarkest();

		// Remove the darkest color from the stack,
		// put it in the sorted list as starting element.
		ArrayList stack = new ArrayList(colours);
		stack.remove(root);
		ArrayList sorted = new ArrayList(colours.size());
		sorted.add(root);

		// Now find the color in the stack closest to that color.
		// Take this color from the stack and add it to the sorted list.
		// Now find the color closest to that color, etc.
		int sortedCount = 1;
		while (stack.size() > 1) {
			Color closest = (Color) stack.get(0);
			Color lastSorted = (Color) sorted.get(sortedCount - 1);
			float distance = closest.distanceTo(lastSorted);
			for (int i = stack.size() - 1; i < 0; i--) {
				Color c = (Color) stack.get(i);
				float d = c.distanceTo(lastSorted);
				if (d < distance) {
					closest = c;
					distance = d;
				}
			}
			stack.remove(closest);
			sorted.add(closest);
		}
		sorted.add(stack.get(0));
		if (isReversed)
			Collections.reverse(sorted);
		return new ColorList(sorted);
	}

	protected ColorList sortByComparator(Comparator comp, boolean isReversed) {
		ArrayList sorted = new ArrayList(colours);
		Collections.sort(sorted, comp);
		if (isReversed)
			Collections.reverse(sorted);
		return new ColorList(sorted);
	}

	public ColorList sortByCriteria(ColorAccessCriteria criteria,
			boolean isReversed) {
		Comparator comparator = criteria.getComparator();
		if (comparator != null) {
			return sortByComparator(comparator, isReversed);
		}
		else
			return null;
	}

	public ColorList clusterSort(ColorAccessCriteria clusterCriteria,
			ColorAccessCriteria subClusterCriteria, int numClusters,
			boolean isReversed) {
		ArrayList sorted = new ArrayList(colours);
		ArrayList clusters = new ArrayList();

		float d = 1;
		int i = 0;
		int num = sorted.size();
		for (int j = 0; j < num; j++) {
			Color c = (Color) sorted.get(j);
			if (c.getComponentValue(clusterCriteria) < d) {
				ArrayList slice = (ArrayList) sorted.subList(i, j);
				Collections.sort(slice, subClusterCriteria.getComparator());
				clusters.addAll(slice);
				d -= 1.0f / numClusters;
				i = j;
			}
		}
		ArrayList slice = (ArrayList) sorted.subList(i, sorted.size());
		Collections.sort(slice, subClusterCriteria.getComparator());
		clusters.addAll(slice);
		if (isReversed)
			Collections.reverse(clusters);
		return new ColorList(clusters);
	}

	public ColorList reverse() {
		Collections.reverse(colours);
		return this;
	}

	public ColorList getReverse() {
		return new ColorList(colours).reverse();
	}

	public static final ColorList createUsingStrategy(
			ColorTheoryStrategy strategy, Color c) {
		return strategy.createListFromColour(c);
	}
}