package net.sixik.steamaudio.gpu;

import net.sixik.steamaudio.core.Context;
import net.sixik.steamaudio.core.SteamAudioException;

/**
 * Wrapper over {@code IPLOpenCLDeviceList}: enumerates the OpenCL devices on
 * the system that satisfy a set of requirements. Use {@link #getDeviceDesc}
 * to pick a device, then {@link #createDevice} to instantiate it.
 * <p>
 * OpenCL is loaded dynamically by Steam Audio; if no OpenCL runtime is
 * installed, creation fails with a {@link SteamAudioException}.
 */
public final class OpenCLDeviceList implements AutoCloseable {

    /** Consider both CPU and GPU OpenCL devices ({@code IPL_OPENCLDEVICETYPE_ANY}). */
    public static final int DEVICE_TYPE_ANY = 0;

    /** Consider only CPU OpenCL devices ({@code IPL_OPENCLDEVICETYPE_CPU}). */
    public static final int DEVICE_TYPE_CPU = 1;

    /** Consider only GPU OpenCL devices ({@code IPL_OPENCLDEVICETYPE_GPU}). */
    public static final int DEVICE_TYPE_GPU = 2;

    /** Opaque pointer to {@code IPLOpenCLDeviceList}; 0 means the list is closed. */
    private long peer;

    /** Opaque pointer to the parent context, needed for device creation. */
    private final long contextPeer;

    /**
     * Describes the properties of an OpenCL device.
     */
    public static final class DeviceDesc {

        /** OpenCL platform name. */
        public String platformName;

        /** OpenCL platform vendor's name. */
        public String platformVendor;

        /** OpenCL platform version. */
        public String platformVersion;

        /** OpenCL device name. */
        public String deviceName;

        /** OpenCL device vendor's name. */
        public String deviceVendor;

        /** OpenCL device version. */
        public String deviceVersion;

        /** Device type: {@code DEVICE_TYPE_ANY/CPU/GPU}. */
        public int type;

        /** Number of CUs reserved for convolution (0 if unsupported). */
        public int numConvolutionCUs;

        /** Number of CUs reserved for IR update (0 if unsupported). */
        public int numIRUpdateCUs;

        /** CU reservation granularity. */
        public int granularity;

        /** Relative performance score of a single CU (supported AMD GPUs only). */
        public float perfScore;

        @Override
        public String toString() {
            return deviceName + " (" + platformName + ", " + deviceVersion + ")";
        }
    }

    /**
     * Creates an OpenCL device list via {@code iplOpenCLDeviceListCreate}.
     *
     * @param context              Steam Audio context
     * @param deviceType           which devices to consider:
     *                             {@code DEVICE_TYPE_ANY/CPU/GPU}
     * @param numCUsToReserve      number of GPU compute units to reserve
     *                             (0 = use the entire GPU)
     * @param fractionCUsForIRUpdate fraction of reserved CUs used for IR
     *                             update (0..1)
     * @param requiresTAN          only list devices supporting TrueAudio Next
     * @throws SteamAudioException   if Steam Audio returns an error (in
     *                               particular, if no OpenCL runtime is installed)
     * @throws IllegalStateException if the context is closed
     */
    public OpenCLDeviceList(Context context, int deviceType, int numCUsToReserve,
                            float fractionCUsForIRUpdate, boolean requiresTAN) {
        if (!context.isOpen()) {
            throw new IllegalStateException("Context is closed");
        }
        this.contextPeer = context.peerForChildren();
        peer = nCreate(contextPeer, deviceType, numCUsToReserve,
                fractionCUsForIRUpdate, requiresTAN);
    }

    /**
     * Checks whether the list is open.
     *
     * @return {@code true} if the list is created and not closed
     */
    public boolean isOpen() {
        return peer != 0;
    }

    /**
     * Returns the number of devices in the list
     * ({@code iplOpenCLDeviceListGetNumDevices}).
     *
     * @return number of devices
     */
    public int getNumDevices() {
        requireOpen();
        return nGetNumDevices(peer);
    }

    /**
     * Retrieves the properties of a device ({@code iplOpenCLDeviceListGetDeviceDesc}).
     *
     * @param index index of the device within the list
     * @return the device descriptor
     */
    public DeviceDesc getDeviceDesc(int index) {
        requireOpen();
        return nGetDeviceDesc(peer, index);
    }

    /**
     * Creates an OpenCL device from the device at the given index
     * ({@code iplOpenCLDeviceCreate}).
     *
     * @param index index of the device within the list
     * @return the created OpenCL device
     * @throws SteamAudioException if Steam Audio returns an error
     */
    public OpenCLDevice createDevice(int index) {
        requireOpen();
        return new OpenCLDevice(nCreateDevice(contextPeer, peer, index));
    }

    /**
     * Releases the list ({@code iplOpenCLDeviceListRelease}). Safe to call
     * multiple times.
     */
    @Override
    public void close() {
        if (peer != 0) {
            nRelease(peer);
            peer = 0;
        }
    }

    private void requireOpen() {
        if (peer == 0) {
            throw new IllegalStateException("OpenCLDeviceList is closed");
        }
    }

    private static native long nCreate(long contextPeer, int deviceType, int numCUsToReserve,
                                       float fractionCUsForIRUpdate, boolean requiresTAN);

    private static native int nGetNumDevices(long peer);

    private static native DeviceDesc nGetDeviceDesc(long peer, int index);

    private static native long nCreateDevice(long contextPeer, long peer, int index);

    private static native void nRelease(long peer);
}
