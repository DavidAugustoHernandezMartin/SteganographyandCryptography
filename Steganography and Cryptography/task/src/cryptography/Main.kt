package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.Path
import javax.imageio.ImageIO

/** The Steganography and Cryptography program is an interface that allows
    any user to encrypt and hide a message in a PNG image. It consists of
    three main commands: "hide","show" and "exit".

    The "hide" command requests a path to access a PNG image that will be
    used for Steganography.Once the image and the exit route are obtained,
    a message to hide and a password are requested. The message will be
    encrypted byte by byte using an XOR operation with the password, and
    then saved bit by bit for each of the least significant bits of each of
    the RGB encoding bytes of the pixels.

    The "show" command will ask the user for the image path with the hidden
    message and the password to decrypt it. Once the requirements are given,
    the text that should correspond to the decrypted message with the given
    password will be displayed (not necessarily the correct one).

    Finally, the "exit" command quits the program.

    All these activities are being performed under a loop process that also
    contains a try-catch wrapper for the most eventual exceptions.
 */
fun main() {
    var play: Boolean = true
    var input: String = ""
    do{
        try {
            println("Task (hide, show, exit):")
            input = readln()
            var inFile: Path
            var outFile: Path
            var inImage: BufferedImage
            var outImage: BufferedImage
            when (input) {
                "hide" -> {
                    println("Input image file:")
                    inFile = Path(readln())
                    inImage = readBytes(inFile)
                    println("Output image file:")
                    outFile = Path(readln())
                    println("Message to hide:")
                    val message = readln()
                    println("Password:")
                    val password = readln()
                    if (message.length > (inImage.height * inImage.width)/8)
                        throw IllegalArgumentException("The input image is not large enough to hold this message.")
                    outImage = writeMessage(inImage, outFile, message, password)
                    println("Message saved in $outFile image.")
                }
                "show" -> {
                    println("Input image file:")
                    inFile = Path(readln())
                    inImage = readBytes(inFile)
                    println("Password:")
                    val password = readln()
                    val message = readMessage(inImage,password)
                    println("Message:\n$message")
                }
                "exit" -> {
                    println("Bye!")
                    play = false
                }
                else -> println("Wrong task: $input")
            }
        } catch (e: IllegalArgumentException){
            println(e.message)
        } catch (e: NoSuchFileException ) {
            println("The referenced file can't be read!: ${e.message}")
        } catch (e: FileAlreadyExistsException) {
            println("The referenced file can't be written!: ${e.message}")
        } catch (e: NullPointerException) {
            println("The value given can't be null!: ${e.message}")
        } catch (e: Exception) {
            println("An error has occurred, please try again:${e.message}")
        }
    } while(play)
}

fun readBytes(file: Path): BufferedImage  {
    val originalImage = ImageIO.read(file.toFile())
    // Creating a new BufferedImage without alfa channel
    val newImage = BufferedImage(originalImage.width, originalImage.height, BufferedImage.TYPE_INT_RGB)
    // Copy all the pixels from the original image to the new one, discarding the alfa channel
    for (y in 0 until originalImage.height) {
        for (x in 0 until originalImage.width) {
            val originalColor = Color(originalImage.getRGB(x, y), true) // Leer el color con alfa
            // Create a new color without alfa channel
            val rgbColor = Color(originalColor.red, originalColor.green, originalColor.blue)
            newImage.setRGB(x, y, rgbColor.rgb) // Finally, assigning the RGB value to each pixel
        }
    }
    return newImage
}


fun writeMessage(image: BufferedImage, file: Path, message: String, password: String): BufferedImage {
    //Both the message and the password must be encoded to byteArray
    val passwordBytes = password.encodeToByteArray()
    val messageBytes = message.encodeToByteArray()
        .let {
            /*Now, we apply the encryption transformation to
            * each message byte with each password byte in order.
            * If the password is smaller than the message length
            * it's counter will be reset to the first byte. */
            var j = 0
            for(i in 0 until it.size){
                if(j == password.length) j = 0
                it[i] = (it[i].toInt() xor passwordBytes[j++].toInt()).toByte()
        }
            //Finally, the exit sequence is added to the encrypted message.
        it + byteArrayOf(0b00000000, 0b00000000, 0b00000011)
    }
    //The current bit power
    var bitIndex = 7
    val max = messageBytes.size
    //The current byte
    var byteIndex = 0
    var color: Color

    //This is the loop for writing every lsb from each byte
    write@ for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            color = Color(image.getRGB(x, y))
            /*Because the message bits will be written from the msb to the lsb,
            * The process for placing each bit in the image will require to
            * displace the current bit the number of places corresponding to
            * the current bit power, without counting the sign bit from the Int,
            * and then extracting the lsb with the AND operation*/
            val bit = ((messageBytes[byteIndex].toInt() ushr bitIndex--) and 0b00000001).toInt()
            /*Once we have the byte with the data well-placed on the lsb, we assign it to the
            * lsb of the blue color from the current pixel. First, the AND operation nulls
            * the lsb and then the OR operator assigns the value of the lsb from
            * the data byte to it. */
            val newBlue = (color.blue and 0b11111110) or bit
            val newColor = Color(color.red, color.green, newBlue)
            image.setRGB(x, y, newColor.rgb)
            if (bitIndex < 0) {
                //We make sure that the process breaks once all the bytes are written.
                if (++byteIndex == max) break@write
                bitIndex = 7
            }
        }
    }
    ImageIO.write(image, "png", file.toFile())
    return image
}

fun readMessage(image: BufferedImage, password: String): String {
    val passwordBytes = password.encodeToByteArray()
    val exitSequence = byteArrayOf(0b00000000, 0b00000000, 0b00000011)
    var bitIndex = 7
    var currentByte = 0
    val messageBytes = mutableListOf<Byte>()

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val color = Color(image.getRGB(x, y))
            //First, the lsb is extracted
            val blueBit = (color.blue and 0b00000001)
            /*Then, the respective lsb value will be
            decoded and assigned to the current byte
            for the current bit location.*/
            currentByte = currentByte or (blueBit shl bitIndex--)
            /*If the bitIndex variable gets lower than 0, we've reached
            the limit of the byte we're reading, and we'll need to add it
            to the messageBytes collection*/
            if (bitIndex < 0) {
                messageBytes.add(currentByte.toByte())
                bitIndex = 7
                currentByte = 0

                // Check here for exit sequence
                if (messageBytes.size >= 3 &&
                    messageBytes[messageBytes.size - 3] == exitSequence[0] &&
                    messageBytes[messageBytes.size - 2] == exitSequence[1] &&
                    messageBytes[messageBytes.size - 1] == exitSequence[2]
                ) {
                    // Remove exit sequence and convert to string
                    return messageBytes.dropLast(3).toByteArray().let {
                        /*Before converting them into a string, the bytes
                        * must be decoded from the encryption by
                        * matching them with their respective byte signatures
                        * in the provided password, with the XOR
                        * operation conversion.*/
                        var j = 0
                        for(i in 0 until it.size){
                            if(j == password.length) j = 0
                            it[i] = (it[i].toInt() xor passwordBytes[j++].toInt()).toByte()
                        }
                        it
                    }.toString(Charsets.UTF_8)
                }
            }
        }
    }

    // If we reach here, we didn't find the exit sequence
    return messageBytes.toByteArray().toString(Charsets.UTF_8)
}

// Extension function to convert List<UByte> to ByteArray
//fun List<UByte>.toByteArray(): ByteArray = this.map { it.toByte() }.toByteArray()