/*
 * Muzikant <http://muzikant-android.blogspot.fr/2011/02/how-to-get-root-access-and-execute.html>
 */
package me.shkschneider.dropbearserver2.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.RootTools;

public abstract class ShellUtils {
	private static final String CMD_RM = "rm";
	private static final String CMD_CHOWN = "chown";
	private static final String CMD_CHMOD = "chmod";
	private static final String CMD_MV = "mv";
	private static final String CMD_CP = "cp";
	private static final String CMD_KILL = "kill";

	public static final Boolean execute(String command) {
		CommandCapture commands = new CommandCapture(0, command);
		try {
			RootTools.getShell(true).add(commands);
			return true;
		}
		catch (IOException e) {
			L.e("IOException: " + e.getMessage());
		}
		catch (TimeoutException e) {
			L.e("TimeoutException: " + e.getMessage());
		}
		catch (RootDeniedException e)
		{
			L.e("Root Access Denied: " + e.getMessage());
		}
		return false;
	}

	public static final Boolean chown(String path, String owner) {
		return execute(CMD_CHOWN + " " + owner + " " + path);
	}

	public static final Boolean chmod(String path, String chmod) {
		return execute(CMD_CHMOD + " " + chmod + " " + path);
	}

	private static final Boolean rm(String path) {
		return execute(CMD_RM + " " + path);
	}

	public static final Boolean mv(String srcPath, String destPath) {
		return execute(CMD_MV + " " + srcPath + " " + destPath);
	}

	public static final Boolean cp(String srcPath, String destPath) {
		return execute(CMD_CP + " " + srcPath + " " + destPath);
	}

	public static final Boolean kill(String processId) {
		return execute(CMD_KILL + " " + processId);
	}

	public static final Boolean killPidFile(String pidFile) {
		return execute(CMD_KILL + " $(cat " + pidFile + ")");
	}

	public static final Boolean killall(String processName) {
		return execute("for pid in $(ps "
				+ processName
				+ " | awk '/"
				+ processName
				+ "$/ {print $2}'); do kill $pid; done");
	}

	public static final Boolean remountReadWrite(String path) {
		return RootTools.remount(path, "RW");
	}

	public static final Boolean remountReadOnly(String path) {
		return RootTools.remount(path, "RO");
	}

    public static Boolean deleteFile(String fileName)
    {
    	Boolean bDeleted = false;

    	if (RootUtils.hasRootAccess)
    	{
    		bDeleted = ShellUtils.rm(fileName);
    	}
    	else
    	{
	    	try
	    	{
	    		File file = new File(fileName);
	    		bDeleted = file.delete();
	    	}
	    	catch(Exception e)
	    	{
	    		bDeleted = false;
	    	}
    	}

    	return bDeleted;
    }

	/**
	 *
	    DEScribe - A Discrete Experience Sampling cross platform application
	    Copyright (C) 2011
	    Sébastien Faure <sebastien.faure3@gmail.com>,
	    Bertrand Gros   <gros.bertrand@gmail.com>,
	    Yannick Prie    <yannick.prie@univ-lyon1.fr>.

	    This program is free software: you can redistribute it and/or modify
	    it under the terms of the GNU General Public License as published by
	    the Free Software Foundation, either version 3 of the License, or
	    (at your option) any later version.

	    This program is distributed in the hope that it will be useful,
	    but WITHOUT ANY WARRANTY; without even the implied warranty of
	    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	    GNU General Public License for more details.

	    You should have received a copy of the GNU General Public License
	    along with this program.  If not, see <http://www.gnu.org/licenses/>.
	 *
	 */

	/**
	 * copie le fichier source dans le fichier resultat retourne vrai si cela
	 * réussit
	 */
	public static Boolean copyFile(File source, File dest)
	{
		try
		{
			// Declaration et ouverture des flux
			java.io.FileInputStream sourceFile = new java.io.FileInputStream(
					source);

			try
			{
				java.io.FileOutputStream destinationFile = null;

				try
				{
					destinationFile = new FileOutputStream(dest);

					// Lecture par segment de 0.5Mo
					byte buffer[] = new byte[512 * 1024];
					int nbLecture;

					while ((nbLecture = sourceFile.read(buffer)) != -1)
					{
						destinationFile.write(buffer, 0, nbLecture);
					}
				}
				finally
				{
					destinationFile.close();
				}
			}
			finally
			{
				sourceFile.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false; // Erreur
		}

		return true; // Résultat OK
	}

	/**
	 * Déplace le fichier source dans le fichier résultat
	 */
	public static Boolean moveFile(File source, File destination)
	{
		if (!destination.exists())
		{
			// On essaye avec renameTo
			boolean result = source.renameTo(destination);
			if (!result)
			{
				// On essaye de copier
				result = true;
				result &= copyFile(source, destination);
				if (result)
					result &= source.delete();

			}
			return (result);
		}
		else
		{
			// Si le fichier destination existe, on annule ...
			return (false);
		}
	}
}
