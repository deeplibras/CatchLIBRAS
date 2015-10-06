package br.edu.ifsp.coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.primesense.nite.JointType;
import com.primesense.nite.Point2D;
import com.primesense.nite.Skeleton;
import com.primesense.nite.SkeletonJoint;
import com.primesense.nite.SkeletonState;
import com.primesense.nite.UserData;
import com.primesense.nite.UserTracker;
import com.primesense.nite.UserTrackerFrameRef;;

public class BodyCoordinate implements InterfaceCoordinate, UserTracker.NewFrameListener {

	public static final int X = 0, Y = 1, Z = 2;
	public static final int REAL_WORLD = 10, DEPTH = 11;
	public static final int HEAD = 0, NECK = 1, LEFT_SHOULDER = 2, RIGHT_SHOULDER = 3, LEFT_ELBOW = 4, RIGHT_ELBOW = 5,
			LEFT_HAND = 6, RIGHT_HAND = 7, TORSO = 8, LEFT_HIP = 9, RIGHT_HIP = 10, LEFT_KNEE = 11, RIGHT_KNEE = 12,
			LEFT_FOOT = 13, RIGHT_FOOT = 14;

	private UserTracker userTracker = null;
	private Map<Short, List<float[][]>> coordinates = null;
	private boolean realWorld = true;
	private boolean startRecordingUsers = false;
	private SimpleViewer view = null;
	
	public BodyCoordinate(UserTracker userTracker){
		this(userTracker, null);
	}

	public BodyCoordinate(UserTracker userTracker, SimpleViewer view) {
		this.coordinates = this.createMapStructure();
		this.userTracker = userTracker;
		this.view = view;
	}

	@Override
	public synchronized void onNewFrame(UserTracker userTracker) {
		UserTrackerFrameRef frame = userTracker.readFrame();

		List<UserData> users = frame.getUsers();
		for (UserData user : users) {
			if (!isUserReadyToTrack(user)) {
				continue;
			}

			float[][] joints = trackingUser(user);
			
			if (!startRecordingUsers) {
				return;
			}
			
			System.out.print("");
			for (float[] fs : joints) {
				System.out.print(Arrays.toString(fs));
			}
			System.out.println();

			
			
			List<float[][]> userMoves = coordinates.get(user.getId());

			if (userMoves == null) {
				userMoves = this.createListStructure();
				coordinates.put(user.getId(), userMoves);
			}

			userMoves.add(joints);
		}
	}

	@Override
	public void startRecordingUsers() {
		startRecordingUsers = true;
	}

	@Override
	public void stopRecordingUsers() {
		startRecordingUsers = false;
	}

	@Override
	public Map<Short, List<float[][]>> getMovimentsList() {
		return coordinates;
	}

	@Override
	public Map<Short, float[][][]> getMovimentsArray() {
		Map<Short, float[][][]> newCoordinates = new HashMap<>();
		List<float[][]> list;
		float[][][] array;

		for (Short userID : coordinates.keySet()) {
			list = coordinates.get(userID);
			array = list.toArray(new float[list.size()][JointType.values().length][3]);
			newCoordinates.put(userID, array);
		}
		return newCoordinates;
	}

	public void setCoordinateSystem(int coordinate) {
		switch (coordinate) {
		case REAL_WORLD:
			realWorld = true;
			break;
		case DEPTH:
			realWorld = false;
			break;
		}
	}

	private List<float[][]> createListStructure() {
		// return new CopyOnWriteArrayList<>();
		return new ArrayList<float[][]>();
	}

	private Map<Short, List<float[][]>> createMapStructure() {
		// return new ConcurrentHashMap<Short, List<float[][]>>();
		return new HashMap<Short, List<float[][]>>();
	}

	private boolean isUserReadyToTrack(UserData user) {
		if (user.isLost()) {
			userTracker.stopSkeletonTracking(user.getId());
			return false;
		}
		if (!user.isVisible()) {
			return false;
		}
		userTracker.startSkeletonTracking(user.getId());
		if (user.isNew()) {
			userTracker.startSkeletonTracking(user.getId());
			return false;
		}
		if (user.getSkeleton().getState() != SkeletonState.TRACKED) {
			return false;
		}
		return true;
	}

	private float[][] trackingUser(UserData user) {
		Skeleton skeleton = user.getSkeleton();
		JointType[] jointTypes = JointType.values();
		float[][] joints = new float[jointTypes.length][3];
		float[][] depth = new float[jointTypes.length][3];

		for (int i = 0; i < jointTypes.length; i++) {
			SkeletonJoint joint = skeleton.getJoint(jointTypes[i]);

			// float[] system = converterCoordinateSystem(joint.getPosition());

			float[] system;
			Point2D<Float> pointDepth = userTracker.convertJointCoordinatesToDepth(joint.getPosition());

			depth[i][X] = pointDepth.getX();
			depth[i][Y] = pointDepth.getY();
			depth[i][Z] = joint.getPosition().getZ();

			if (realWorld) {
				system = new float[3];
				system[X] = joint.getPosition().getX();
				system[Y] = joint.getPosition().getX();
				system[Z] = joint.getPosition().getX();
			} else {
				system = depth[i];
			}

			joints[i][X] = system[X];
			joints[i][Y] = system[Y];
			joints[i][Z] = system[Z];
		}
		
		if (view != null){
			view.addMoviments(depth);
		}
		
		return joints;
	}
}