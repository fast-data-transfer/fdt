
package lia.util.net.copy;

import java.io.File;


class FileReaderSessionHelper {
    static final File getdCacheFile(final String fName) throws Exception {
        return new edu.caltech.hep.dcapj.dCacheFile(fName, edu.caltech.hep.dcapj.dCacheFile.Mode.READ_ONLY);
    }

}
