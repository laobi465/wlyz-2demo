// 极策k C# SDK 加密工具
// 作者: 极策k  日期: 2026-07-21
using System;
using System.Security.Cryptography;
using System.Text;

namespace Jicek.Sdk
{
    public static class CryptoUtil
    {
        public static string HmacSha256Base64(string data, string secret)
        {
            using var mac = new HMACSHA256(Encoding.UTF8.GetBytes(secret));
            var signBytes = mac.ComputeHash(Encoding.UTF8.GetBytes(data));
            return Convert.ToBase64String(signBytes);
        }

        public static string Sha256Hex(string input)
        {
            using var sha = SHA256.Create();
            var bytes = sha.ComputeHash(Encoding.UTF8.GetBytes(input));
            var sb = new StringBuilder(64);
            foreach (var b in bytes) sb.Append(b.ToString("x2"));
            return sb.ToString();
        }

        /// <summary>RSA-2048-OAEP 加密（卡密传输），返回 Base64</summary>
        public static string RsaEncryptOaep(string plaintext, string publicKeyB64)
        {
            var pubBytes = Convert.FromBase64String(publicKeyB64);
            using var rsa = RSA.Create();
            rsa.ImportSubjectPublicKeyInfo(pubBytes, out _);
            var cipher = rsa.Encrypt(Encoding.UTF8.GetBytes(plaintext),
                RSAEncryptionPadding.OaepSHA256);
            return Convert.ToBase64String(cipher);
        }
    }
}
