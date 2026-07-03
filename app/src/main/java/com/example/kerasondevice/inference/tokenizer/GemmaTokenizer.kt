package com.example.kerasondevice.inference.tokenizer

import android.util.Log
import com.github.google.sentencepiece.SentencePieceProcessor
import java.io.File

/**
 * Wraps the SentencePiece tokenizer for the Gemma model.
 * Loads vocabulary from an explicit file on first use.
 */
class GemmaTokenizer(vocabFile: File) {

    private val processor = SentencePieceProcessor()

    val vocabSize: Int
    val bosId: Int
    val eosId: Int
    val padId: Int
    val unkId: Int
    val endOfTurnId: Int

    init {
        require(vocabFile.exists() && vocabFile.canRead()) {
            "vocabulary.spm not found or unreadable: ${vocabFile.absolutePath}"
        }
        processor.loadOrDie(vocabFile.absolutePath)
        vocabSize = processor.pieceSize
        bosId = processor.bosId()
        eosId = processor.eosId()
        padId = processor.padId()
        unkId = processor.unkId()
        endOfTurnId = processor.pieceToId(END_OF_TURN_TOKEN)
        Log.d(TAG, "Loaded SPM vocabSize=$vocabSize bos=$bosId eos=$eosId pad=$padId unk=$unkId eot=$endOfTurnId")
    }

    fun tokenize(text: String): List<Int> =
        processor.encodeAsIds(text).toList()

    fun detokenize(tokenIds: List<Int>): String {
        val filtered = tokenIds.filter { it != padId && it != bosId && it != eosId }
        if (filtered.isEmpty()) return ""
        return processor.decodeIds(*filtered.toIntArray())
    }

    companion object {
        private const val TAG = "GemmaTokenizer"
        private const val END_OF_TURN_TOKEN = "<end_of_turn>"
        const val SEQUENCE_LENGTH = 128
    }
}
