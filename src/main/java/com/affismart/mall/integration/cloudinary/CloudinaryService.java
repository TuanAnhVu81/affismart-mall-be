package com.affismart.mall.integration.cloudinary;

import com.affismart.mall.common.error.ErrorCode;
import com.affismart.mall.exception.AppException;
import com.cloudinary.Cloudinary;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryService {

	private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

	private final ObjectProvider<Cloudinary> cloudinaryProvider;
	private final CloudinaryProperties cloudinaryProperties;

	public CloudinaryService(
			ObjectProvider<Cloudinary> cloudinaryProvider,
			CloudinaryProperties cloudinaryProperties
	) {
		this.cloudinaryProvider = cloudinaryProvider;
		this.cloudinaryProperties = cloudinaryProperties;
	}

	public String uploadProductImage(MultipartFile file) {
		validateUploadInput(file);

		Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
		if (!cloudinaryProperties.isEnabled() || cloudinary == null) {
			throw new AppException(
					ErrorCode.FILE_STORAGE_NOT_CONFIGURED,
					"Cloudinary is not enabled. Set CLOUDINARY_ENABLED=true and provide credentials."
			);
		}

		Map<String, Object> options = new HashMap<>();
		if (StringUtils.hasText(cloudinaryProperties.getFolder())) {
			options.put("folder", cloudinaryProperties.getFolder());
		}
		options.put("resource_type", "image");

		try {
			Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
			Object secureUrl = uploadResult.get("secure_url");
			if (secureUrl == null || !StringUtils.hasText(secureUrl.toString())) {
				throw new AppException(ErrorCode.FILE_UPLOAD_FAILED, "Cloudinary did not return secure_url");
			}
			return secureUrl.toString();
		} catch (IOException exception) {
			log.error("Failed to upload product image to Cloudinary", exception);
			throw new AppException(ErrorCode.FILE_UPLOAD_FAILED, "Product image upload failed");
		}
	}

	private void validateUploadInput(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Image file is required");
		}

		String contentType = file.getContentType();
		if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
			throw new AppException(ErrorCode.INVALID_INPUT, "Only image files are supported");
		}
	}
}
