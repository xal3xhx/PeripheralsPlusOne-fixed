package com.austinv11.peripheralsplusplus.mount;

import com.austinv11.peripheralsplusplus.PeripheralsPlusPlus;
import com.austinv11.peripheralsplusplus.lua.api.DynApi;
import com.austinv11.peripheralsplusplus.reference.Reference;
import com.austinv11.peripheralsplusplus.utils.ReflectionHelper;
import com.austinv11.peripheralsplusplus.utils.proxy.PeripheralChangeListener;
import com.google.gson.*;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.util.*;

public class DynamicMount {

	/**
	 * Mount the programs that support the specified peripheral
	 * @param computer the computer to mount the directories on
	 * @param peripheral the peripheral that must be supported by the program(s)
	 * @return paths mounted
	 */
	private static List<String> mount(IComputerAccess computer, IPeripheral peripheral) {
		List<String> attached = new ArrayList<>();
		File mountDirectory = new File(getMountDirectory(), String.valueOf(computer.getID()));
		File installed = new File(mountDirectory, "installed");
		File installedIndex = new File(installed, "index.json");
		if (!installedIndex.isFile())
			return attached;
		FileReader indexReader;
		try {
			indexReader = new FileReader(installedIndex);
		}
		catch (FileNotFoundException e) {
			return attached;
		}
		JsonArray index;
		try {
			index = new Gson().fromJson(indexReader, JsonArray.class);
		}
		catch (JsonIOException | JsonSyntaxException e) {
			return attached;
		}
		Map<String, File> allExtraFiles = new HashMap<>();
		for (JsonElement programElement : index) {
			if (!(programElement instanceof JsonObject))
				continue;
			JsonObject program = programElement.getAsJsonObject();
			// Name
			JsonElement nameElement = program.get("name");
			if (!(nameElement instanceof JsonPrimitive))
				continue;
			String name = nameElement.getAsString();
			// Peripherals supported
			JsonElement peripheralsElement = program.get("peripherals");
			if (!(peripheralsElement instanceof JsonArray))
				continue;
			JsonArray peripheralsSupported = peripheralsElement.getAsJsonArray();
			// Extra files
			JsonElement extraElement = program.get("extra");
			JsonArray extraFiles = new JsonArray();
			if (extraElement instanceof JsonArray)
				extraFiles = extraElement.getAsJsonArray();
			// Check if the peripheral is supported
			boolean supported = false;
			for (JsonElement peripheralElement : peripheralsSupported) {
				if (!(peripheralElement instanceof JsonPrimitive))
					continue;
				String supportedType = peripheralElement.getAsString();
				String[] peripheralNameSplit = supportedType.split("\\*");
				String peripheralName = peripheral.getType();
				if ((peripheralNameSplit.length == 0 && (peripheralName.equals(supportedType) ||
							supportedType.equals("*"))) ||
						(peripheralNameSplit.length > 0 && peripheralName.startsWith(peripheralNameSplit[0]) &&
						(peripheralNameSplit.length < 2 || peripheralName.endsWith(peripheralNameSplit[1])))
						) {
					supported = true;
					break;
				}
			}
			if (!supported)
				continue;
			// Mount the files
			File programDirectory = new File(installed, name);
			File programLua = new File(programDirectory, name + ".lua");
			File programHelp = new File(programDirectory, name + ".txt");
			if (!programLua.isFile())
				continue;
			String path = String.format("/rom/programs/%s.lua", name);
			path = computer.mount(path, new DynamicMountFile(programLua));
			if (path != null)
				attached.add(path);
			else
				PeripheralsPlusPlus.LOGGER.debug(String.format(
						"Failed to mount program \"%s\". The name may be already taken.",
						name
				));
			if (programHelp.isFile()) {
				path = String.format("/rom/help/%s.txt", name);
				path = computer.mount(path, new DynamicMountFile(programHelp));
				if (path != null)
					attached.add(path);
				else
					PeripheralsPlusPlus.LOGGER.debug(String.format(
							"Failed to mount help file for program \"%s\".",
							name
					));
			}
			// Add extra files to the main map
			for (JsonElement extraFileElement : extraFiles) {
				if (!(extraFileElement instanceof JsonPrimitive))
					continue;
				String extraFilePath = extraFileElement.getAsString();
				File extraFile = Paths.get(programDirectory.toString(), "extra", extraFilePath).toFile();
				if (!extraFile.isFile())
					continue;
				path = String.format("%s/%s", name, extraFilePath);
				allExtraFiles.put(path, extraFile);
			}
		}
		// Mount extra files
		allExtraFiles.put("dyn/.", installedIndex); // Force the dyn directory to map
		String path = String.format("/rom/programs/%s", Reference.MOD_ID);
		path = computer.mount(path, new DynamicMountExtra(allExtraFiles));
		if (path != null)
			attached.add(path);
		else
			PeripheralsPlusPlus.LOGGER.debug("Failed to mount extra files.");
		return attached;
	}

	private static File getMountDirectory() {
		return Paths.get(
				DimensionManager.getWorld(0).getSaveHandler().getWorldDirectory().toString(),
				Reference.MOD_ID,
				"mount"
		).toFile();
	}

	/**
	 * Helper to aid peripherals to easily attach the mount
	 * @param computer computer to attach to
	 * @param peripheral the peripheral requesting the attach
	 * @return list of paths attached
	 */
	public static List<String> attach(IComputerAccess computer, IPeripheral peripheral) {
		try {
			Object apiEnvironment = ReflectionHelper.getApiEnvironment(computer.getID());
			if (apiEnvironment != null) {
				ReflectionHelper.registerAPI(computer.getID(),
						new DynApi(apiEnvironment));
			}
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException |
				InvocationTargetException ignore) {}
		List<String> attached = new ArrayList<>();
		IMount dynScript = ComputerCraftAPI.createResourceMount(PeripheralsPlusPlus.class, Reference.MOD_ID,
				"lua/mount/dyn.lua");
		IMount dynHelp = ComputerCraftAPI.createResourceMount(PeripheralsPlusPlus.class, Reference.MOD_ID,
				"lua/mount/dyn.txt");
		IMount jsonApi = ComputerCraftAPI.createResourceMount(PeripheralsPlusPlus.class, Reference.MOD_ID,
				"lua/mount/json.lua");
		IMount jsonApiHelp = ComputerCraftAPI.createResourceMount(PeripheralsPlusPlus.class, Reference.MOD_ID,
				"lua/mount/json.txt");
		try {
			if (dynScript != null) {
				String path = computer.mount("/rom/programs/dyn.lua", dynScript);
				if (path != null)
					attached.add(path);
			}
			if (dynHelp != null) {
				String path = computer.mount("/rom/help/dyn.txt", dynHelp);
				if (path != null)
					attached.add(path);
			}
			if (jsonApi != null) {
				String path = computer.mount(String.format(
						"/rom/programs/%s/dyn/json.lua",
						Reference.MOD_ID
				), jsonApi);
				if (path != null)
					attached.add(path);
			}
			if (jsonApiHelp != null) {
				String path = computer.mount("/rom/help/json.txt", jsonApiHelp);
				if (path != null)
					attached.add(path);
			}
			File mountDirectory = new File(getMountDirectory(), String.valueOf(computer.getID()));
			if (!mountDirectory.exists())
				if (!mountDirectory.mkdirs())
					PeripheralsPlusPlus.LOGGER.error("Failed to create mount directory.");
			String path = computer.mountWritable("/." + Reference.MOD_ID,
					new DynamicMountWritable(mountDirectory));
			if (path != null)
				attached.add(path);
			attached.addAll(mount(computer, peripheral));
		} catch (RuntimeException e) {
			PeripheralsPlusPlus.LOGGER.error(e);
		}
		return attached;
	}

	/**
	 * Helper function that attempts to detach a passed list of paths
	 * Passed list is cleared
	 * @param computer computer to detach from
	 */
	public static void detach(IComputerAccess computer, List<String> paths) {
		for (String path : paths)
			try {
				computer.unmount(path);
			} catch (RuntimeException ignore) {}
		paths.clear();
	}

	/**
	 * Handle checking the computer registry for computers that do not have the dynamic mount and attempt to mount it
	 * @param event server tick
	 */
	@SubscribeEvent
	public void serverTick(TickEvent.WorldTickEvent event) {
		if (event.side.isClient())
			return;
		try {
			Collection<Object> computers = ReflectionHelper.getServerComputers();
			for (Object computer : computers) {
				Object peripheralChangeListener = ReflectionHelper.getPeripheralChangeListener(computer);
				if (peripheralChangeListener != null && (!Proxy.isProxyClass(peripheralChangeListener.getClass()) ||
						!(Proxy.getInvocationHandler(peripheralChangeListener) instanceof PeripheralChangeListener))) {
					DynamicMountPeripheralChangeListener dynamicMountPeripheralChangeListener =
							new DynamicMountPeripheralChangeListener(computer, peripheralChangeListener);
					if (dynamicMountPeripheralChangeListener.initializePeripherals())
						ReflectionHelper.setPeripheralChangeListener(computer, dynamicMountPeripheralChangeListener);
				}
			}
		}
		catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException |
				InvocationTargetException ignore) {}
	}
}
