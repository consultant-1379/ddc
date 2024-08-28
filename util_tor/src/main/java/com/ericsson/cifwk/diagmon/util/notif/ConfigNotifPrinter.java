package com.ericsson.cifwk.diagmon.util.notif;

import com.ericsson.cifwk.diagmon.util.common.AnyPrinter;
import com.ericsson.nms.umts.ranos.cms.nead.segmentserver.neaccess.cello_p1.idl.ConfigExtended.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;


public class ConfigNotifPrinter {
    private static final SimpleDateFormat m_DF = new SimpleDateFormat("dd-MM-yy:HH:mm:ss.SSS");

    private ConfigNotifPrinter() {}

    public static void printEvent(final PrintStream output,
                                  final ConfigNotification configNotif,
                                  final String source, final long timeStamp) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream printOut = new PrintStream(baos);

        try {
            String timeStampStr;
            synchronized (m_DF) {
                timeStampStr = m_DF.format(new Date(timeStamp));
            }
            printOut.print(timeStampStr + "\t" + source);
            switch (configNotif.discriminator().value()) {
                case NotificationKind._AVCType: {
                    final AVCNotification notif = configNotif.updatedMO();

                    printOut.println("\t" + notif.generationCount + "\tAVC");
                    printOut.println("\t" + notif.moInformation.prefix + "," + notif.moInformation.localDN);
                    for (int j = 0; j < notif.changedAttributes.length; j++) {
                        printOut.print("\t\t" + notif.changedAttributes[j].name + ": ");
                        printOut.println(AnyPrinter.toString(notif.changedAttributes[j].newValue));
                    }
                    break;
                }

                case NotificationKind._MOCreatedType: {
                    final TopologyNotification notif = configNotif.createdMO();
                    printOut.println("\t" + notif.generationCount + "\tTopology\tCREATED");
                    printOut.println("\t" + notif.moInformation.prefix + "," + notif.moInformation.localDN);
                    break;
                }

                case NotificationKind._MODeletedType: {
                    final TopologyNotification notif = configNotif.deletedMO();
                    printOut.println("\t" + notif.generationCount + "\tTopology\tDELETED");
                    printOut.println("\t" + notif.moInformation.prefix + "," + notif.moInformation.localDN);
                    break;
                }

                case NotificationKind._OverflowType: {
                    printOut.println("\tOverflowType");
                    break;
                }

                default: {
                    printOut.println("NOTIF_ERROR: Unknown notif type " + configNotif);
                }

            }

            printOut.println();
            output.print(baos.toString());
        } catch (Exception e) {
            output.println(e);
            e.printStackTrace(output);
        }
    }
}
