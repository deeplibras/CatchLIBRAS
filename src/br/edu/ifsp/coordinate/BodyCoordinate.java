package br.edu.ifsp.coordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.primesense.nite.JointType;
import com.primesense.nite.NiTE;
import com.primesense.nite.Point2D;
import com.primesense.nite.PoseType;
import com.primesense.nite.Skeleton;
import com.primesense.nite.SkeletonJoint;
import com.primesense.nite.SkeletonState;
import com.primesense.nite.UserData;
import com.primesense.nite.UserTracker;
import com.primesense.nite.UserTrackerFrameRef;

import br.edu.ifsp.coordinates.ComponentViewer;
import br.edu.ifsp.coordinates.InterfaceCoordinate;;

/**
 * This is the class responsible for recording the movements of the users. It
 * receives the coordinates of the users supplied by the middleware NiTE, and
 * stores this coordinates for posterior use.
 * 
 * @author Matheus da Silva Ferreira
 *
 */
public class BodyCoordinate implements InterfaceCoordinate, UserTracker.NewFrameListener {

	public static final int X = 0, Y = 1, Z = 2;
	public static final int REAL_WORLD = 10, DEPTH = 11;
	public static final int HEAD = 0, NECK = 1, LEFT_SHOULDER = 2, RIGHT_SHOULDER = 3, LEFT_ELBOW = 4, RIGHT_ELBOW = 5,
			LEFT_HAND = 6, RIGHT_HAND = 7, TORSO = 8, LEFT_HIP = 9, RIGHT_HIP = 10, LEFT_KNEE = 11, RIGHT_KNEE = 12,
			LEFT_FOOT = 13, RIGHT_FOOT = 14;

	private UserTracker userTracker = null;
	private UserTrackerFrameRef frame;
	private Map<Short, List<Float[][]>> coordinates = null;
	private boolean realWorld = true;
	private boolean startRecordingUsers = false;
	private ComponentViewer view = null;
	
	/**
	 * Default Constructor
	 */
	public BodyCoordinate() {
		this(null);
	}

	/**
	 * This Constructor allows you to set a {@link ComponentViewer} object. In
	 * this sense, every time that a new skeleton was tracked, the object
	 * informed will receive the new data.
	 * 
	 * @param view
	 *            The object used to show the images created by the sensor.
	 */
	public BodyCoordinate(ComponentViewer view) {
		// OpenNI.initialize();
		NiTE.initialize();

		this.coordinates = this.createMapStructure();
		this.userTracker = UserTracker.create();
		this.view = view;

		userTracker.addNewFrameListener(this);
	}

	/**
	 * Set method of the {@link ComponentViewer} object. This method allows you
	 * to set the object used to display the images created by the sensor, so
	 * this object will receive the new data automatically.
	 * 
	 * @param view
	 *            The object used to show the images created by the sensor. Set
	 *            null to remove the object.
	 */
	public void setView(ComponentViewer view) {
		this.view = view;
	}

	/**
	 * Method called every time that a new frame was created by the NiTE
	 * middleware.
	 */
	@Override
	public synchronized void onNewFrame(UserTracker userTracker) {
		this.userTracker = userTracker;
		/*
		 * Get the object that stores all the information of the currently frame
		 * that NiTE is able to say
		 */
		this.frame = userTracker.readFrame();
		// System.out.println("NiTE: "+frame.getDepthFrame().getFrameIndex());

		/* For each users detected by the middleware NiTE in this frame */
		List<UserData> users = frame.getUsers();
		for (UserData user : users) {

			/* If the currently user is not ready to be tracked */
			if (!isUserTracked(user)) {
				continue;
			}

			extractJoints(user);
			
		}
	}
	
	public synchronized void extractJoints(UserData user){
		/* Get the joints of the current user */
		Float[][] joints = trackingUser(user);

		/* If the recording is not allowed. */
		if (!startRecordingUsers) {
			return;
		}

		System.out.print("");
		for (Float[] fs : joints) {
			System.out.print(Arrays.toString(fs));
		}
		System.out.println();

		/* Get the movements recorded of the current user. */
		List<Float[][]> userMoves = coordinates.get(user.getId());

		/*
		 * If there is no list which stores the movements of the current
		 * user, so a new one will be created.
		 */
		if (userMoves == null) {
			userMoves = this.createListStructure();
			coordinates.put(user.getId(), userMoves);
		}

		/* Insert the new joints detected in the current frame. */
		userMoves.add(joints);

	}

	/**
	 * Method responsible for starting recording the user movements.
	 * 
	 * @param user
	 *            Current user. It is used when a pose detection has been
	 *            defined.
	 */
	private void startRecording(UserData user) {
		startRecordingUsers = true;
	}

	/**
	 * Method responsible for stopping recording the user movements.
	 * 
	 * @param user
	 *            Current user. It is used when a pose detection has been
	 *            defined.
	 */
	private void stopRecording(UserData user) {
		startRecordingUsers = false;
	}

	/**
	 * Start recording the user movements right away
	 */
	@Override
	public void startRecordingUsers() {
		for (UserData user : frame.getUsers()) {
			startRecording(user);
		}
	}

	/**
	 * Stop recording the user movements right away
	 */
	@Override
	public void stopRecordingUsers() {
		for (UserData user : frame.getUsers()) {
			stopRecording(user);
		}
	}

	@Override
	public Map<Short, List<Float[][]>> getMovimentsList() {
		return coordinates;
	}

	@Override
	public Map<Short, Float[][][]> getMovimentsArray() {
		Map<Short, Float[][][]> newCoordinates = new HashMap<>();
		List<Float[][]> list;
		Float[][][] array;

		for (Short userID : coordinates.keySet()) {
			list = coordinates.get(userID);
			array = list.toArray(new Float[list.size()][JointType.values().length][3]);
			newCoordinates.put(userID, array);
		}
		return newCoordinates;
	}

	/**
	 * Get the amount of frames stored. This method identify what user has more
	 * frames stored, and return the amount of frames for this user.
	 * 
	 * @return The amount of frames stored to the user who has more frames
	 */
	public int getFramesCount() {
		int max = 0;
		for (List<Float[][]> userFrames : coordinates.values()) {
			if (max < userFrames.size()) {
				max = userFrames.size();
			}
		}
		return max;
	}

	/**
	 * Delete all the movements stored. This method clean the movements that was
	 * stored.
	 */
	public void clearMoviments() {
		coordinates = createMapStructure();
	}

	/**
	 * Set the coordinate system used to store the coordinates of the users'
	 * joints. There are two coordinate system used: Real World and Depth. Check
	 * the OpenNI Documentation for more details.
	 * 
	 * @param coordinate
	 *            The coordinate system used. Please, use the constants
	 *            {@link BodyCoordinate}.REAL_WORLD or {@link BodyCoordinate}
	 *            .DEPTH.
	 */
	/*public void setCoordinateSystem(int coordinate) {
		switch (coordinate) {
		case REAL_WORLD:
			realWorld = true;
			break;
		case DEPTH:
			realWorld = false;
			break;
		}
	}*/

	/**
	 * Method used to create the default structure to store the movements of a
	 * user.
	 * 
	 * @return The List which store the movements of only one user.
	 */
	private List<Float[][]> createListStructure() {
		// return new CopyOnWriteArrayList<>();
		return new ArrayList<Float[][]>();
	}

	/**
	 * Method used to create the default structure to store the movements of all
	 * user.
	 * 
	 * @return The Map which store the movements of all user.
	 */
	private Map<Short, List<Float[][]>> createMapStructure() {
		// return new ConcurrentHashMap<Short, List<Float[][]>>();
		return new HashMap<Short, List<Float[][]>>();
	}

	/**
	 * Check if the user informed was already tracked.
	 * 
	 * @param user
	 *            User to check.
	 * @return true if the user was tracked, false otherwise.
	 */
	private boolean isUserTracked(UserData user) {

		/*
		 * Check if the user was in the previous frame, but no longer in this
		 * one
		 */
		if (user.isLost()) {

			/* Stop tracking this user */
			userTracker.stopSkeletonTracking(user.getId());

			return false;
		}

		/* If the user is not visible */
		if (!user.isVisible()) {
			return false;
		}

		/* Start tracking this user */
		userTracker.startSkeletonTracking(user.getId());

		/* Check if the user was not in the previous frame */
		if (user.isNew()) {

			/* Start tracking this user */
			userTracker.startSkeletonTracking(user.getId());

			return false;
		}

		/* If the user was not tracked yet */
		if (user.getSkeleton().getState() != SkeletonState.TRACKED) {
			return false;
		}
		return true;
	}

	/**
	 * Get the position of the joints of the informed user. This method return
	 * the coordinates, according to the coordinate system informed, of the
	 * joints of the informed user. Moreover, this method also feed the
	 * {@link ComponentViewer} object with the newest data.
	 * 
	 * @param user
	 *            The current user The current user whose coordinates will be
	 *            returned.
	 * @return An array representing the coordinates, where: Float[] represents
	 *         the joints; Float[][] represents the positions in the axis X, Y
	 *         and Z
	 */
	private Float[][] trackingUser(UserData user) {
		/* Get the skeleton of the current user */
		Skeleton skeleton = user.getSkeleton();

		/* Temporary structure */
		JointType[] jointTypes = JointType.values();
		Float[][] joints = new Float[jointTypes.length][3];
		Float[][] depth = new Float[jointTypes.length][3];

		/* For each joint available in the middleware NiTE */
		for (int i = 0; i < jointTypes.length; i++) {

			/* Get the current joint of the current user */
			SkeletonJoint joint = skeleton.getJoint(jointTypes[i]);

			// Float[] system = converterCoordinateSystem(joint.getPosition());

			Float[] system;
			Point2D<Float> pointDepth;

			/*
			 * Check if it is necessary to convert the coordinate system. It is
			 * necessary when the client request the data in depth format, or
			 * when a {@link ComponentViewer} object has been supplied. The
			 * converted values will be stored in a temporary structure.
			 */
			if (!realWorld || view != null) {
				pointDepth = userTracker.convertJointCoordinatesToDepth(joint.getPosition());
				depth[i][X] = pointDepth.getX();
				depth[i][Y] = pointDepth.getY();
				depth[i][Z] = joint.getPosition().getZ();
			}

			/*
			 * Return the data in the coordinate format that the client wanted.
			 * If it is in real world, the system will return the values
			 * received by the middleware NiTE. Otherwise, it will return the
			 * converted values which are stored in the temporary structure.
			 */
			if (realWorld) {
				system = new Float[3];
				system[X] = joint.getPosition().getX();
				system[Y] = joint.getPosition().getY();
				system[Z] = joint.getPosition().getZ();
			} else {
				system = depth[i];
			}

			joints[i][X] = system[X];
			joints[i][Y] = system[Y];
			joints[i][Z] = system[Z];
		}

		/*
		 * If a {@link ComponentViewer} object has been supplied, the system
		 * will feed this object with the newest data.
		 */
		if (view != null) {
			view.addUserMoviments(depth);
		}

		return joints;
	}

}