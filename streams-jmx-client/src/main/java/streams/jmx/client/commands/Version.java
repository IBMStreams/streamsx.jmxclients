package streams.jmx.client.commands;

import com.beust.jcommander.Parameters;
import streams.jmx.client.Constants;

@Parameters(commandDescription = Constants.DESC_VERSION)
public class Version implements Command {

        public String getName() {
                return Constants.CMD_VERSION;
        }

        public String getHelp() {
                return Constants.DESC_VERSION;
        }

        public CommandResult execute() {
                return new CommandResult(Version.getTitleAndVersionString());
        }

        // Get value of Implementation-Title and Version from jar MANIFEST.mf file
        // This needs to be in the outer-most jar file
        // In this build, we create a jar-with-depencencies, so it needs
        // to be in that jar file
        static public String getImplementationVersion() {
                //Package p = getClass().getPackage();
                Package p = Version.class.getPackage();
                StringBuilder str = new StringBuilder();
                String version = p.getImplementationVersion();
                                if (version != null && version.length() > 0)
                                        str.append(version);
                                else
                                        str.append("not specified");
                return str.toString();
        }

        static public String getImplementationTitle() {
                Package p = Version.class.getPackage();
                StringBuilder str = new StringBuilder();
                String title = p.getImplementationTitle();
                                if (title != null && title.length() > 0)
                                        str.append(title);
                                else
                                        str.append(Constants.PROGRAM_NAME);
                return str.toString();
        }

        static public String getTitleAndVersionString() {
                StringBuilder str = new StringBuilder();
                str.append(getImplementationTitle());
                str.append(" ");
                str.append(getImplementationVersion());
                return str.toString();
        }	

        static public void main(String[] args) {
		//System.out.println("Version: " + getImplementationVersion());
		System.out.println(getTitleAndVersionString());
	}
}
