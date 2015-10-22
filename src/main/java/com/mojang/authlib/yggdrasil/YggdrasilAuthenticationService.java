package com.mojang.authlib.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;

import net.minecraft.launcher.Launcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilAuthenticationService
  extends HttpAuthenticationService
{
  private final String clientToken;
  private final Gson gson;
  private static final Logger LOGGER;
  
  public YggdrasilAuthenticationService(Proxy proxy, String clientToken)
  {
    super(proxy);
    this.clientToken = clientToken;
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
    builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
    builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
    this.gson = builder.create();
  }
  
  public UserAuthentication createUserAuthentication(Agent agent)
  {
    return new YggdrasilUserAuthentication(this, agent);
  }
  
  public MinecraftSessionService createMinecraftSessionService()
  {
    return new YggdrasilMinecraftSessionService(this);
  }
  
  public GameProfileRepository createProfileRepository()
  {
    return new YggdrasilGameProfileRepository(this);
  }
  
  protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT,String username)
    throws AuthenticationException
  {
	if (Launcher.isOnlineMode()) {
    try
    {
      String jsonResult = input == null ? performGetRequest(url) : performPostRequest(url, this.gson.toJson(input), "application/json");
      T result = (T)this.gson.fromJson(jsonResult, classOfT);
      if (result == null) {
        return null;
      }
      if (StringUtils.isNotBlank(result.getError()))
      {
        if ("UserMigratedException".equals(result.getCause())) {
          throw new UserMigratedException(result.getErrorMessage());
        }
        if (result.getError().equals("ForbiddenOperationException")) {
          throw new InvalidCredentialsException(result.getErrorMessage());
        }
        throw new AuthenticationException(result.getErrorMessage());
      }
      return result;
    }
    catch (IOException e)
    {
      throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
    }
    catch (IllegalStateException e)
    {
      throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
    }
    catch (JsonParseException e)
    {
      throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
    }
	} else {
        LOGGER.info("Player's UUID is {}", getUUID(username));
        String uuidn = getUUID(username);
		 String jsonResult="{\n" +
                 "    \"accessToken\": \"68e6e1faeb3e442097e1b45d2b3a577e\",\n" +
                 "    \"clientToken\": \""+getClientToken()+"\",\n" +
                 "    \"selectedProfile\": {\"name\":\""+username+"\",\"id\":\""+uuidn+"\"},\n" +
                 "    \"availableProfiles\":[{\"name\":\""+username+"\",\"id\":\""+uuidn+"\"}],\n" +
                 "    \"error\": \"\",\n" +
                 "    \"errorMessage\":\"\"\n" +
                 "    \n" +
                 "}" ;
         final T result = this.gson.fromJson(jsonResult, classOfT);
         return result;
	}
  }
    
  
  public String getClientToken()
  {
    return this.clientToken;
  }
  
  private static class GameProfileSerializer
    implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile>
  {
    public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException
    {
      JsonObject object = (JsonObject)json;
      UUID id = object.has("id") ? (UUID)context.deserialize(object.get("id"), UUID.class) : null;
      String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
      return new GameProfile(id, name);
    }
    
    public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context)
    {
      JsonObject result = new JsonObject();
      if (src.getId() != null) {
        result.add("id", context.serialize(src.getId()));
      }
      if (src.getName() != null) {
        result.addProperty("name", src.getName());
      }
      return result;
    }
  }
  public String getUUID(String username) throws AuthenticationException{
      String uuid = "00000000-0000-0000-0000-000000000000";     
      try
      {
          URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username); 
          String jsonResult =  performGetRequest(url);
          Profile result = this.gson.fromJson(jsonResult, Profile.class);
          if(result != null)
          uuid = result.getId();
      }
      catch (IOException e)
      {
        throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
      }
      catch (IllegalStateException e)
      {
        throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
      }
      catch (JsonParseException e)
      {
        throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
      }     
      return uuid;
  }
  public class Profile {
        private String name;
        private String id;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getId() {
            return id;
        }
    }
  static {
      LOGGER = LogManager.getLogger();
  }

}