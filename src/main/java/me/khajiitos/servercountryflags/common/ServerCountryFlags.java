package me.khajiitos.servercountryflags.common;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Subdivision;
import com.mojang.blaze3d.platform.NativeImage;
import me.khajiitos.servercountryflags.common.config.Config;
import me.khajiitos.servercountryflags.common.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerCountryFlags {
	public static final String MOD_ID = "servercountryflags";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static HashMap<String, CityResponse> servers = new HashMap<>(); // Servers' flags
	public static HashMap<String, Float> flagAspectRatios = new HashMap<>();
	public static Set<String> unknownCountryCodes = new HashSet<>();
	public static ServerRedirectHandler redirectResolver = ServerRedirectHandler.createDnsSrvRedirectHandler();

	private static final File SCF_FOLDER = new File(Minecraft.getInstance().gameDirectory, MOD_ID);
	private static DatabaseReader cityReader = null;

	static <T>
	T last(T[] arr) {
		return arr[arr.length - 1];
	}

	public static void init() {
		File database = new File(SCF_FOLDER, "GeoLite2-City.mmdb");
		try {
			cityReader = new DatabaseReader.Builder(database)
					.withCache(new CHMCache())
					.build();
		} catch (IOException e) {
			LOGGER.info("Could not open the GeoLite database file!");
		}

		Config.init();
		Minecraft.getInstance().execute(() -> {
			ResourceManager resourceManager =  Minecraft.getInstance().getResourceManager();
			Map<ResourceLocation, Resource> resourceLocations = resourceManager.listResources("textures/gui/flags", path -> true);

			Thread flagThread = new Thread(() -> {
				for (Map.Entry<ResourceLocation, Resource> entry : resourceLocations.entrySet()) {
					if (!entry.getKey().getNamespace().equals(MOD_ID)) {
						continue;
					}
					try {
						if (entry.getValue() == null) {
							ServerCountryFlags.LOGGER.error("Failed to load resource " + entry.getKey().getPath());
							continue;
						}

						try (InputStream inputStream = entry.getValue().open()) {
							NativeImage image = NativeImage.read(inputStream);
							String code = last(entry.getKey().getPath().split("/"));
							code = code.substring(0, code.length() - 4);
							flagAspectRatios.put(code, (float)image.getWidth() / (float)image.getHeight());
						}
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
					}
				}
			});
			flagThread.setName("Flag load thread");
			flagThread.start();
		});
	}

	public static CityResponse getMaxmindResponse(String ip) {
		if (cityReader == null) return null;
		try {
			InetAddress ipAddress = InetAddress.getByName(ip);
			return cityReader.city(ipAddress);
		} catch (UnknownHostException e) {
			LOGGER.info("Invalid IP: " + ip);
			return null;
		} catch (IOException | GeoIp2Exception e) {
			LOGGER.info("Failed to lookup " + ip);
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isIpLocal(InetAddress address) {
		return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress();
	}

	public static void updateServerLocationInfo(String serverAddress) {
		final String cleanServerAddress;
		// Prefixes used by the Raknetify mod
		if (serverAddress.startsWith("raknet;")) {
			cleanServerAddress = serverAddress.substring("raknet;".length());
		} else if (serverAddress.startsWith("raknetl;")) {
			cleanServerAddress = serverAddress.substring("raknetl;".length());
		} else {
			cleanServerAddress = serverAddress;
		}

		new Thread(() -> {
			ServerAddress address = ServerAddress.parseString(cleanServerAddress);
			if (Config.cfg.resolveRedirects) {
				Optional<ServerAddress> redirect = redirectResolver.lookupRedirect(address);

				if (redirect.isPresent()) {
					address = redirect.get();
				}
			}

			Optional<ResolvedServerAddress> resolvedAddress = ServerAddressResolver.SYSTEM.resolve(address);
			if (resolvedAddress.isPresent()) {
				InetSocketAddress socketAddress = resolvedAddress.get().asInetSocketAddress();
				String stringHostAddress = isIpLocal(socketAddress.getAddress()) ? "" : socketAddress.getAddress().getHostAddress();
				if (stringHostAddress.isEmpty()) return;

				CityResponse response = getMaxmindResponse(stringHostAddress);
				if (response != null) {
					servers.put(serverAddress, response);
				}
			}
		}).start();
	}

	public static FlagRenderInfo getFlagRenderInfo(CityResponse apiResponse) {
		String countryCode;
		double aspectRatio;
		List<FormattedCharSequence> tooltip = new ArrayList<>();

		if (apiResponse == null) {
			if (!Config.cfg.displayUnknownFlag) {
				return null;
			}
			tooltip.add(Component.translatable("servercountryflags.locationInfo.unknown").getVisualOrderText());
			countryCode = "unknown";
			aspectRatio = 1.5;
		} else {
			// for some reason, the iso code can be null but the country won't be null
			if (apiResponse.getCountry().getIsoCode() == null) {
				if (!Config.cfg.displayUnknownFlag) {
					return null;
				}
				tooltip.add(Component.translatable("servercountryflags.locationInfo.unknown").getVisualOrderText());
				countryCode = "unknown";
				aspectRatio = 1.5;
				return new FlagRenderInfo(countryCode, aspectRatio, tooltip);
			}

			String cCode = apiResponse.getCountry().getIsoCode().toLowerCase();
			if (ServerCountryFlags.flagAspectRatios.containsKey(cCode)) {
				countryCode = cCode;
				aspectRatio = ServerCountryFlags.flagAspectRatios.get(countryCode);
			} else {
				if (!ServerCountryFlags.unknownCountryCodes.contains(cCode)) {
					ServerCountryFlags.LOGGER.error("Unknown country code: " + cCode);
					ServerCountryFlags.unknownCountryCodes.add(cCode);
				}

				if (Config.cfg.displayUnknownFlag) {
					countryCode = "unknown";
					aspectRatio = 1.5;
				} else {
					return null;
				}
			}

			tooltip.add(Component.literal(getTooltipText(apiResponse)).getVisualOrderText());
		}

		return new FlagRenderInfo(countryCode, aspectRatio, tooltip);
	}

	private static @NotNull String getTooltipText(CityResponse apiResponse) {
		StringBuilder division = new StringBuilder();

		List<Subdivision> subdivisions = apiResponse.getSubdivisions();
		for (int i = 0; i < subdivisions.size(); i++) {
			division.append(subdivisions.get(i).getName());
			if (i != subdivisions.size() - 1) {
				division.append(", ");
			}
		}

		String city = apiResponse.getCity().getName();

		String tooltipText = "";
		if (Config.cfg.showDistrict && !division.isEmpty()) {
			tooltipText += division;
			tooltipText += ", ";
		}

		if (city != null) {
			tooltipText += city;
			tooltipText += ", ";
		}

		tooltipText += apiResponse.getCountry().getName();
		return tooltipText;
	}
}