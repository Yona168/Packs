package com.github.yona168.packs;

import monotheistic.mongoose.core.utils.FileUtils;
import net.minecraft.server.v1_13_R2.NBTCompressedStreamTools;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Class.forName;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getServer;

public class BukkitSerializers {
  private static final String ERR_NBT_LOAD = "Failed to find a load method in NBTCompressedStreamTools";
  private static final String ERR_NBT_SAVE = "Failed to find a save method in NBTCompressedStreamTools";

  private static final Constructor<?> CONSTRUCTOR_NBT;
  private static final Method METHOD_NBT_SAVE;
  private static final Method METHOD_NBT_LOAD;
  private static final Method METHOD_NBT_SET_STRING;
  private static final Method METHOD_NBT_GET_STRING;
  private static final Method METHOD_NBT_SET_BYTE_ARRAY;
  private static final Method METHOD_NBT_GET_BYTE_ARRAY;
  private static final Method METHOD_NBT_SET_INT;
  private static final Method METHOD_NBT_GET_INT;

  private static final Class<?> CLASS_ITEM;
  protected static final Class<?> CLASS_CRAFT_ITEM;
  private static final Constructor<?> CONSTRUCTOR_ITEM;
  private static final Method METHOD_ITEM_TO;
  private static final Method METHOD_ITEM_FROM;
  private static final Method METHOD_ITEM_SAVE;
  private static final Method METHOD_ITEM_GET_TAG;
  private static final Method METHOD_ITEM_SET_TAG;
  private static final InventoryHolder INVENTORY_HOLDER = () -> null;


  static {
    try {
      final String version = getServer().getClass().getName().split("\\.")[3];
      final String nms = "net.minecraft.server.%s.%s";
      final String cb = "org.bukkit.craftbukkit.%s.%s";

      final Class<?> nbt = Class(nms, version, "NBTTagCompound");
      CONSTRUCTOR_NBT = nbt.getConstructor();
      CONSTRUCTOR_NBT.setAccessible(true);
      METHOD_NBT_SET_STRING = nbt.getDeclaredMethod("setString", String.class, String.class);
      METHOD_NBT_GET_STRING = nbt.getDeclaredMethod("getString", String.class);
      METHOD_NBT_GET_INT = nbt.getDeclaredMethod("getInt", String.class);
      METHOD_NBT_SET_INT = nbt.getDeclaredMethod("setInt", String.class, int.class);
      METHOD_NBT_SET_BYTE_ARRAY = nbt.getDeclaredMethod("setByteArray", String.class, byte[].class);
      METHOD_NBT_GET_BYTE_ARRAY = nbt.getDeclaredMethod("getByteArray", String.class);
      METHOD_NBT_GET_BYTE_ARRAY.setAccessible(true);
      METHOD_NBT_SET_INT.setAccessible(true);
      METHOD_NBT_GET_INT.setAccessible(true);
      METHOD_NBT_SET_BYTE_ARRAY.setAccessible(true);
      Method nbtSave = null, nbtLoad = null;
      for (Method method : Class(nms, version, "NBTCompressedStreamTools").getDeclaredMethods()) {
        final Class<?>[] params = method.getParameterTypes();
        if (params.length == 2 && params[1] == DataOutput.class)
          nbtSave = method;
        else if (params.length == 1 && params[0] == DataInputStream.class)
          nbtLoad = method;
      }
      if ((METHOD_NBT_SAVE = nbtSave) == null)
        throw new IllegalStateException(ERR_NBT_SAVE);
      if ((METHOD_NBT_LOAD = nbtLoad) == null)
        throw new IllegalStateException(ERR_NBT_LOAD);
      METHOD_NBT_SAVE.setAccessible(true);
      METHOD_NBT_LOAD.setAccessible(true);
      CLASS_ITEM = Class(nms, version, "ItemStack");
      CLASS_CRAFT_ITEM = Class(cb, version, "inventory.CraftItemStack");
      METHOD_ITEM_FROM = CLASS_CRAFT_ITEM.getDeclaredMethod("asBukkitCopy", CLASS_ITEM);
      METHOD_ITEM_TO = CLASS_CRAFT_ITEM.getDeclaredMethod("asNMSCopy", ItemStack.class);
      CONSTRUCTOR_ITEM = CLASS_ITEM.getDeclaredConstructor(nbt);
      CONSTRUCTOR_ITEM.setAccessible(true);
      METHOD_ITEM_SAVE = CLASS_ITEM.getDeclaredMethod("save", nbt);
      METHOD_ITEM_SAVE.setAccessible(true);
      METHOD_ITEM_GET_TAG = CLASS_ITEM.getDeclaredMethod("getTag");
      METHOD_ITEM_SET_TAG = CLASS_ITEM.getDeclaredMethod("setTag", nbt);
      METHOD_ITEM_SET_TAG.setAccessible(true);
      METHOD_ITEM_GET_TAG.setAccessible(true);

    } catch (Exception e) {
      throw new IllegalStateException("Could not initialize reflection!", e);
    }
  }

  public static Object getOrCreateNBTTagCompound(ItemStack itemStack) throws Exception {
    Object item = METHOD_ITEM_TO.invoke(null, itemStack);
    Object nbt = METHOD_ITEM_GET_TAG.invoke(item);
    if (nbt == null)
      nbt = createNBTTagCompound();
    METHOD_ITEM_SET_TAG.invoke(item, nbt);
    return nbt;
  }

  public static ItemStack serializeInventoryToItem(ItemStack subject, int size, ItemStack[] items) throws Exception {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Object nbt = getOrCreateNBTTagCompound(subject);
    saveItems(items, outputStream);
    setByteArray(nbt, "yona168_pack_items", outputStream.toByteArray());
    outputStream.close();
    setInt(nbt, "yona168_pack_items_size", size);
    Object itemTo = METHOD_ITEM_TO.invoke(null, subject);
    METHOD_ITEM_SET_TAG.invoke(itemTo, nbt);
    return (ItemStack) METHOD_ITEM_FROM.invoke(null, itemTo);
  }

  public static ItemStack serializeInventoryToItem(ItemStack subject, Inventory inv) throws Exception {
    return serializeInventoryToItem(subject, inv.getSize(), inv.getContents());
  }

  public static Optional<Inventory> getInventoryFromItem(ItemStack subject, String name) throws Exception {
    final Object nbt = METHOD_ITEM_GET_TAG.invoke(METHOD_ITEM_TO.invoke(null, subject));
    if (nbt == null)
      return empty();
    final Optional<byte[]> packItems = getByteArray(nbt, "yona168_pack_items");
    if (!packItems.isPresent())
      return empty();
    InputStream inputStream;
    final ItemStack[] itemStacks = loadItems(inputStream = new ByteArrayInputStream(packItems.get()));
    inputStream.close();
    if (itemStacks == null)
      return empty();
    final Optional<Integer> size = getInt(nbt, "yona168_pack_items_size");
    if (!size.isPresent())
      return empty();
    final Inventory inv = Bukkit.createInventory(INVENTORY_HOLDER, size.get(), name);
    inv.setContents(itemStacks);
    return Optional.of(inv);
  }

  public static Optional<Inventory> getInventoryFromItem(ItemStack subject) throws Exception {
    String name = subject.getItemMeta().getDisplayName();
    return getInventoryFromItem(subject, name == null || name.isEmpty() ? "Pack" : name);
  }

  public static boolean isASerializedInventory(Inventory inv) {
    return inv.getHolder() == INVENTORY_HOLDER;
  }

  public static Optional<Integer> getSize(ItemStack item) throws Exception {
    final Object nbt = METHOD_ITEM_GET_TAG.invoke(METHOD_ITEM_TO.invoke(null, item));
    if (nbt == null)
      return empty();
    return getInt(nbt, "yona168_pack_items_size");
  }

  private static Class<?> Class(String format, String version, String name) throws ClassNotFoundException {
    return forName(format(format, version, name));
  }

  public static Object createNBTTagCompound() throws Exception {
    return CONSTRUCTOR_NBT.newInstance();
  }

  public static void saveNBT(OutputStream out, Object nbt) throws Exception {
    METHOD_NBT_SAVE.invoke(null, nbt, new DataOutputStream(out));
  }

  public static Object loadNBT(InputStream in) throws Exception {
    return METHOD_NBT_LOAD.invoke(null, new DataInputStream(in));
  }

  public static void setByteArray(Object nbt, String key, byte[] val) throws Exception {
    METHOD_NBT_SET_BYTE_ARRAY.invoke(nbt, key, val);
  }

  public static Optional<byte[]> getByteArray(Object nbt, String key) throws Exception {
    final Object result = METHOD_NBT_GET_BYTE_ARRAY.invoke(nbt, key);
    if (result instanceof byte[])
      return Optional.of((byte[]) result);
    return empty();
  }

  public static ItemStack setString(ItemStack item, String key, String value) throws Exception {
    Object nbt=METHOD_ITEM_TO.invoke(null, item);
    setString(METHOD_ITEM_GET_TAG.invoke(nbt), key, value);
    return (ItemStack)METHOD_ITEM_FROM.invoke(null,nbt);
  }

  public static Optional<String> getString(ItemStack item, String key) throws Exception {
    return getString(METHOD_ITEM_GET_TAG.invoke(METHOD_ITEM_TO.invoke(null, item)), key);
  }

  public static Optional<String> getString(Object nbt, String key) throws Exception {
    final Object result = METHOD_NBT_GET_STRING.invoke(nbt, key);
    if (result instanceof String) {
      return Optional.of((String) result);
    } else return Optional.empty();
  }

  public static void setString(Object nbt, String key, String value) throws Exception {
    METHOD_NBT_SET_STRING.invoke(nbt, key, value);
  }

  public static void setInt(Object nbt, String key, int val) throws Exception {
    METHOD_NBT_SET_INT.invoke(nbt, key, val);
  }

  public static Optional<Integer> getInt(Object nbt, String key) throws Exception {
    final Object result = METHOD_NBT_GET_INT.invoke(nbt, key);
    if (result instanceof Integer)
      return Optional.of((Integer) result);
    return empty();
  }

  public static Object itemFromBukkit(ItemStack item) throws Exception {
    return METHOD_ITEM_TO.invoke(null, item);
  }

  public static ItemStack itemToBukkit(Object item) throws Exception {
    return (ItemStack) METHOD_ITEM_FROM.invoke(null, item);
  }

  public static void saveItems(ItemStack[] contents, OutputStream out) throws Exception {
    final ByteBuffer length = ByteBuffer.allocate(4);
    length.putInt(contents.length);
    out.write(length.array());
    for (ItemStack item : contents)
      saveItem(item, out);

  }

  public static ItemStack[] loadItems(InputStream in) throws Exception {
    final ByteBuffer length = ByteBuffer.allocate(4);
    in.read(length.array());
    final ItemStack[] contents = new ItemStack[length.getInt()];
    for (int i = 0; i < contents.length; i++)
      contents[i] = loadItem(in);
    return contents;
  }


  public static void saveItem(ItemStack item, OutputStream out) throws Exception {
    saveNBT(out, METHOD_ITEM_SAVE.invoke(itemFromBukkit(item), createNBTTagCompound()));
  }

  public static ItemStack loadItem(InputStream in) throws Exception {
    return itemToBukkit(CONSTRUCTOR_ITEM.newInstance(loadNBT(in)));
  }
}