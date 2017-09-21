/*
 * Created on Nov 19, 2012
 */
package lia.util.net.common;

import java.util.StringTokenizer;


/**
 * @author ramiro
 */
public final class FDTVersion implements Comparable<FDTVersion> {

    final int major;
    final int minor;
    final int maintenance;

    final String releaseDate;

    /**
     * @param major
     * @param minor
     * @param maintenance
     * @param releaseDate
     */
    private FDTVersion(int major, int minor, int maintenance, String releaseDate) {
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        this.releaseDate = releaseDate;
    }


    public static FDTVersion fromVersionString(final String versionString) {
        if (versionString == null) {
            throw new NullPointerException("Null version string");
        }

        final int rDateDelim = versionString.indexOf('-');
        final String vString = (rDateDelim > 0) ? versionString.substring(0, rDateDelim) : versionString;
        final String rDate = (rDateDelim < 0) ? "" : versionString.substring(rDateDelim + 1);

        final StringTokenizer st = new StringTokenizer(vString, ".");
        int major = 0;
        int minor = 0;
        int maint = 0;

        if (st.hasMoreTokens()) {
            major = Integer.parseInt(st.nextToken());
        }
        if (st.hasMoreTokens()) {
            minor = Integer.parseInt(st.nextToken());
        }
        if (st.hasMoreTokens()) {
            maint = Integer.parseInt(st.nextToken());
        }

        return new FDTVersion(major, minor, maint, rDate);
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FDTVersion [")
                .append(major)
                .append(".")
                .append(minor)
                .append(".")
                .append(maintenance)
                .append("-")
                .append(releaseDate)
                .append("]");
        return builder.toString();
    }


    @Override
    public int compareTo(FDTVersion other) {
        int d = this.major - other.major;
        if (d == 0) {
            d = this.minor - other.minor;
            if (d == 0) {
                d = this.maintenance - other.maintenance;
                if (d == 0) {
                    if (this.releaseDate != null && other.releaseDate != null) {
                        return this.releaseDate.compareTo(other.releaseDate);
                    }
                }
            }
        }
        return d;
    }

}
