package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.Path
import javax.imageio.ImageIO

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
                    outImage = writeMessage(inImage, outFile, message)
                    println("Message saved in $outFile image.")
                }

                "show" -> {
                    println("Input image file:")
                    inFile = Path(readln())
                    inImage = readBytes(inFile)
                    val message = readMessage(inImage)
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
        } catch (e:FileAlreadyExistsException) {
            println("The referenced file can't be written!: ${e.message}")
        } catch (e: NullPointerException) {
            println("The value given can't be null!: ${e.message}")
        } catch (e: Exception) {
            println("An error has occurred, please try again:${e.message}")
        }
    } while(play)
}

fun readBytes(file: Path): BufferedImage = ImageIO.read(file.toFile())

@OptIn(ExperimentalUnsignedTypes::class)
fun writeMessage(image: BufferedImage, file: Path, message: String): BufferedImage {
    val messageBytes = message.toByteArray(StandardCharsets.UTF_8).asUByteArray() + ubyteArrayOf(0u, 0u, 3u)

    if (messageBytes.size > image.height * image.width)
        throw IllegalArgumentException("The input image is not large enough to hold this message.")

    var bitIndex = 0
    val max = messageBytes.size
    var byteIndex = 0
    var color: Color

    write@ for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            color = Color(image.getRGB(x, y))
            val bit = ((messageBytes[byteIndex].toUInt() shr bitIndex++) and 1u).toInt()
            val newBlue = (color.blue and 0xFE) or bit
            val newColor = Color(color.red, color.green, newBlue)
            image.setRGB(x, y, newColor.rgb)
            if (bitIndex == 8) {
                if (++byteIndex == max) break@write
                bitIndex = 0
            }
        }
    }

    ImageIO.write(image, "png", file.toFile())
    return image
}

@OptIn(ExperimentalUnsignedTypes::class)
fun readMessage(image: BufferedImage): String {
    val exitSequence = ubyteArrayOf(0u, 0u, 3u)
    var bitIndex = 0
    var currentByte = 0u
    val messageBytes = mutableListOf<UByte>()

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val color = Color(image.getRGB(x, y))
            val blueBit = (color.blue and 0x01).toUByte()
            currentByte = currentByte or (blueBit.toUInt() shl bitIndex)
            bitIndex++

            if (bitIndex == 8) {
                messageBytes.add(currentByte.toUByte())
                bitIndex = 0
                currentByte = 0u

                // Check for exit sequence
                if (messageBytes.size >= 3 &&
                    messageBytes[messageBytes.size - 3] == exitSequence[0] &&
                    messageBytes[messageBytes.size - 2] == exitSequence[1] &&
                    messageBytes[messageBytes.size - 1] == exitSequence[2]
                ) {
                    // Remove exit sequence and convert to string
                    return messageBytes.dropLast(3).toByteArray().toString(StandardCharsets.UTF_8)
                }
            }
        }
    }

    // If we reach here, we didn't find the exit sequence
    return messageBytes.toByteArray().toString(StandardCharsets.UTF_8)
}

// Extension function to convert List<UByte> to ByteArray
fun List<UByte>.toByteArray(): ByteArray = this.map { it.toByte() }.toByteArray()
